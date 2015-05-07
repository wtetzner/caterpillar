package org.bovinegenius.caterpillar;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.bovinegenius.caterpillar.UrlProcessor.UrlAction;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;

public class UrlCollection implements UrlSink {
    private final Map<String,Channel<URI>> urls;
    private final UrlMap<Response> urlMap;

    private UrlCollection(UrlMap<Response> urlMap) {
        Map<String,Channel<URI>> urls = new ConcurrentHashMap<>();
        this.urls = urls;
        this.urlMap = urlMap;
    }

    private static void add(Map<String,Channel<URI>> urls, URI url) throws SuspendExecution {
        Channel<URI> queue = urls.get(url.getHost()); //.computeIfAbsent(url.getHost(), host -> new LinkedBlockingQueue<URI>());
        if (queue == null) {
            throw new RuntimeException(String.format("Unknown host %s in URL %s", url.getHost(), url));
        }
        try {
            queue.send(url);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void add(URI url) throws SuspendExecution {
        add(this.urls, url);
    }

    public UrlProcessor newProcessor(String host, long accessDelay, UrlAction action) {
        Channel<URI> channel = getStreamForHost(host);
        return new UrlProcessor(accessDelay, this.urlMap, this, channel, action);
    }

    private Channel<URI> getStreamForHost(String host) {
        return urls.computeIfAbsent(host, h -> Channels.newChannel(100000, OverflowPolicy.BLOCK, false, false));
    }

    public static UrlCollection of(UrlMap<Response> urlMap) {
        return new UrlCollection(urlMap);
    }

    @Value
    @RequiredArgsConstructor(staticName="of")
    public static class DelayedUrl implements Delayed {
        long delay;
        URI url;

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (o == null) {
                return 1;
            } else {
                long otherMillis = o.getDelay(TimeUnit.MILLISECONDS);
                if (delay < otherMillis) {
                    return -1;
                } else if (delay == otherMillis) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
    }
}
