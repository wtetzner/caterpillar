package org.bovinegenius.caterpillar;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HostDelays implements KnownHosts {
    private final Map<String,HostDelay> delays;

    private HostDelays(Set<Host> hosts) {
        Map<String,HostDelay> hostDelays = new ConcurrentHashMap<String, HostDelay>();
        hosts.forEach(h -> hostDelays.put(h.getHostname(), HostDelay.of(h.getHostname(), h.getAccessDelay())));
        this.delays = hostDelays;
    }

    public DelayedUrl delayedUrl(URI url) {
        String host = url.getHost();
        HostDelay hostDelay = this.delays.get(host);
        if (hostDelay == null) {
            throw new RuntimeException(String.format("Unknown host: %s", host));
        }
        return hostDelay.delayedUrl(url);
    }

    public static HostDelays of(Set<Host> hosts) {
        return new HostDelays(hosts);
    }

    @Override
    public boolean isKnown(String host) {
        return this.delays.containsKey(host);
    }
}
