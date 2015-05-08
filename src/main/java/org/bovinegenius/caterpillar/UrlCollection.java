package org.bovinegenius.caterpillar;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.DelayQueue;

import org.bovinegenius.caterpillar.Crawler.Host;
import org.bovinegenius.caterpillar.UrlProcessor.UrlAction;

public class UrlCollection implements UrlSink {
    private final IdSequence ids;
    private final DelayQueue<DelayedUrl> urls;
    private final HostDelays hostDelays;
    private final Map<URI,Boolean> seen;

    private UrlCollection(Set<Host> hosts, Map<URI,Boolean> seen) {
        this.ids = new IdSequence();
        this.urls = new DelayQueue<>();
        this.seen = seen;
        this.hostDelays = HostDelays.of(hosts);
    }

    private static void add(DelayQueue<DelayedUrl> urls, URI url, HostDelays hostDelays, Map<URI,Boolean> seen) {
        seen.computeIfAbsent(url, l -> {
            urls.add(hostDelays.delayedUrl(l));
            return true;
        });
    }

    @Override
    public void add(URI url) {
        add(this.urls, url, this.hostDelays, this.seen);
    }

    public UrlProcessor newProcessor(UrlAction action) {
        return new UrlProcessor(this.ids.next().toString(), this, this.urls, this.seen, action, this.hostDelays);
    }

    public static UrlCollection of(Set<Host> hosts, Map<URI,Boolean> seen) {
        return new UrlCollection(hosts, seen);
    }
}
