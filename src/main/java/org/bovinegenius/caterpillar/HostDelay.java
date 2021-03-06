package org.bovinegenius.caterpillar;

import java.net.URI;

import lombok.Getter;


public class HostDelay {
    @Getter private final long delay;
    @Getter private final String hostname;
    private final DelaySequence delaySequence;

    private HostDelay(String hostname, long delay) {
        this.delay = delay;
        this.hostname = hostname;
        this.delaySequence = DelaySequence.of(delay);
    }

    public DelayedUrl delayedUrl(URI url) {
        DelayedUrl delayedUrl = DelayedUrl.of(this.delaySequence.next(), url);
        return delayedUrl;
    }

    public static HostDelay of(String hostname, long delay) {
        return new HostDelay(hostname, delay);
    }
}
