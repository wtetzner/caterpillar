package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.Pair.pair;
import static org.bovinegenius.caterpillar.util.UriUtils.uri;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;

import org.bovinegenius.caterpillar.UrlMap.UrlMapResult;
import org.glassfish.jersey.client.ClientProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.strands.channels.Channel;

public class UrlProcessor {
    private final Fiber<Boolean> fiber;
    @Getter private final UrlAction action;
    @Getter private final long accessDelay;

    UrlProcessor(long accessDelay, final UrlMap<Response> urlMap, final UrlSink sink, final Channel<URI> channel, UrlAction action) {
        this.accessDelay = accessDelay;
        this.action = action;
        this.fiber = new Fiber<>(() -> {
            for (URI url; (url = channel.receive()) != null;) {
                process(url, urlMap, sink, action, accessDelay);
            }
            return true;
        });
    }

    @Suspendable
    private static void process(URI uri, UrlMap<Response> urlMap, UrlSink sink, UrlAction action, long delay) throws SuspendExecution {
        UrlMapResult<Response> result = urlMap.getLinks(uri, url -> {
            Response response = lookup(url);
            List<URI> links = extractUrls(response).stream().map(link -> url.resolve(link)).distinct().collect(Collectors.toList());
            return pair(response, links);
        });
        if (!result.isKnown()) {
            Response response = result.getData().get();
            List<URI> links = result.getLinks();
            List<URI> cleanedLinks = links.stream().filter(l -> l.getHost().equalsIgnoreCase(uri.getHost())).collect(Collectors.toList());
            for (URI link : cleanedLinks) {
                sink.add(link);
            }
            action.apply(uri, response);
        }
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

    @Suspendable
    private static Response lookup(URI url) {
        Client client = AsyncClientBuilder.newClient();
        Response response = client.target(url)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
                .request(MediaType.WILDCARD)
                .get();
        response.bufferEntity();
        return response;
    }

    public UrlProcessor start() {
        this.fiber.start();
        return this;
    }

    void join() {
        try {
            //this.fiber.joinNoSuspend();
            this.fiber.join();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static interface UrlAction {
        public void apply(URI uri, Response response) throws SuspendExecution;
    }
}
