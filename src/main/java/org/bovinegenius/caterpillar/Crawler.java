package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.Pair.pair;
import static org.bovinegenius.caterpillar.util.UriUtils.uri;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.bovinegenius.caterpillar.UrlProcessor.UrlAction;

public class Crawler {
    @Getter private final List<UrlProcessor> processors;
    private final List<URI> seedUrls;
    private final UrlCollection collection;

    private Crawler(Map<String,Long> hostDelays, Stream<URI> seedUrls, UrlAction action, int processorsPerHost) {
        Set<Host> hosts = hostsToSet(hostDelays);
        Map<URI,Boolean> seen = new ConcurrentHashMap<URI, Boolean>();
        this.collection = UrlCollection.of(hosts,seen);
        this.processors = hosts.stream()
                .flatMap(h -> processors(this.collection, processorsPerHost, action))
                .collect(Collectors.toList());
        this.seedUrls = seedUrls.collect(Collectors.toList());
    }

    private static Stream<UrlProcessor> processors(UrlCollection collection, int num, UrlAction action) {
        return IntStream.range(0, num).mapToObj(index -> collection.newProcessor(action));
    }

    private static Map<String,Long> hostsToMap(Stream<Host> hosts) {
        Map<String,List<Long>> hostsMap = new HashMap<>();
        hosts.forEach(h -> hostsMap.computeIfAbsent(h.getHostname(), k -> new ArrayList<Long>()).add(h.getAccessDelay()));
        List<Map.Entry<String, List<Long>>> duplicates = hostsMap.entrySet().stream()
                .filter(kv -> kv.getValue().size() > 1)
                .map(e -> pair(e.getKey(), (List<Long>)e.getValue()))
                .collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            throw new RuntimeException(String.format("Duplicate entries found for hosts: %s", duplicates));
        }
        Map<String,Long> hostDelays = new HashMap<>();
        hostsMap.entrySet().stream().forEach(e -> hostDelays.put(e.getKey(), e.getValue().get(0)));
        return Collections.unmodifiableMap(hostDelays);
    }

    private static Set<Host> hostsToSet(Map<String,Long> hostDelays) {
        return Collections.unmodifiableSet(
                hostDelays.entrySet().stream().map(e -> Host.of(e.getKey(), e.getValue())).collect(Collectors.toSet()));
    }

    public void run() {
        for (URI url : seedUrls) {
            collection.add(url);
        }
        this.processors.forEach(p -> p.start());
        this.processors.forEach(p -> p.join());
    }

    public static Crawler of(Stream<Host> hosts, Stream<URI> seedUrls, UrlAction action, int processorsPerHost) {
        return new Crawler(hostsToMap(hosts), seedUrls, action, processorsPerHost);
    }

    public static Crawler of(Stream<Host> hosts, UrlAction action, int processorsPerHost) {
        Map<String,Long> hostDelays = hostsToMap(hosts);
        Set<Host> hostSet = hostsToSet(hostDelays);
        Stream<URI> seedUrls = hostSet.stream().map(h -> uri(String.format("http://%s/", h.getHostname())));
        return new Crawler(hostDelays, seedUrls, action, processorsPerHost);
    }

    @Value
    @RequiredArgsConstructor(staticName="of")
    public static class Host {
        String hostname;
        long accessDelay;
    }
}
