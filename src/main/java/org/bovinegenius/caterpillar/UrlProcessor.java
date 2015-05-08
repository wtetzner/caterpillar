package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.UriUtils.uri;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;

import org.glassfish.jersey.client.ClientProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class UrlProcessor {
    private final Thread thread;
    @Getter private final UrlAction action;

    UrlProcessor(final String name, final UrlSink sink, final DelayQueue<DelayedUrl> channel, final Map<URI,Boolean> seen, UrlAction action, KnownHosts knownHosts) {
        this.action = action;
        this.thread = new Thread(() -> {
            try {
                for (DelayedUrl delayedUrl; (delayedUrl = channel.take()) != null;) {
                    URI url = delayedUrl.getUrl();
                    System.out.println(String.format("(%s) Fetching %s", Instant.now(), delayedUrl));
                    process(url, sink, action, seen, knownHosts, name);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private static void process(URI uri, UrlSink sink, UrlAction action, Map<URI,Boolean> seen, KnownHosts knownHosts, String processorName) {
        Response response = lookup(uri);
        List<URI> links = extractUrls(response).stream().map(link -> uri.resolve(link)).distinct().collect(Collectors.toList());
        List<URI> cleanedLinks = links.stream().filter(l -> knownHosts.isKnown(l.getHost())).collect(Collectors.toList());
        for (URI link : cleanedLinks) {
            sink.add(link);
        }
        action.apply(processorName, uri, response);
    }

    private static String trimFragment(String url) {
        int index = url.lastIndexOf('#');
        if (index >= 0) {
            return url.substring(0, index);
        } else {
            return url;
        }
    }

    private static List<URI> extractUrls(Response response) {
        String contentType = response.getHeaderString("Content-Type");
        if (contentType != null && contentType.startsWith("text/html")) {
            Document doc = Jsoup.parse(response.readEntity(String.class));
            return doc.getElementsByTag("a").stream()
                    .map(e -> e.attr("href"))
                    .map(UrlProcessor::trimFragment)
                    .distinct()
                    .map(l -> uri(l))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Getter(lazy=true) private final static Client client = ClientBuilder.newClient();
    
    private static Response lookup(URI url) {
        Client client = getClient();
        Response response = client.target(url)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
                .request(MediaType.WILDCARD)
                .get();
        response.bufferEntity();
        return response;
    }

    public UrlProcessor start() {
        this.thread.start();
        return this;
    }

    void join() {
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static interface UrlAction {
        public void apply(String processorName, URI uri, Response response);
    }
}
