package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.Collect.list;
import static org.bovinegenius.caterpillar.util.Pair.pair;
import static org.bovinegenius.caterpillar.util.UriUtils.uri;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.bovinegenius.caterpillar.UrlProcessor.UrlAction;

import co.paralleluniverse.fibers.SuspendExecution;

public class Crawler {
    @Getter private final Set<Host> hosts;
    @Getter private final List<UrlProcessor> processors;
    private final List<URI> seedUrls;
    private final UrlCollection collection;

    private Crawler(Set<Host> hosts, Stream<URI> seedUrls, UrlAction action, int processorsPerHost) throws SuspendExecution {
        this.hosts = hosts;
        UrlMap<Response> urlMap = UrlMap.of();

        this.collection = UrlCollection.of(urlMap);
        this.processors = hosts.stream()
                .flatMap(h -> processors(this.collection, processorsPerHost, h.getHostname(), h.getAccessDelay(), action))
                .collect(Collectors.toList());
        this.seedUrls = seedUrls.collect(Collectors.toList());
    }

    private static Stream<UrlProcessor> processors(UrlCollection collection, int num, String host, long accessDelay, UrlAction action) {
        return list(1, 2, 3).stream().map(index -> collection.newProcessor(host, accessDelay, action));
    }

    private static Set<Host> hostsToSet(Stream<Host> hosts) {
        Map<String,List<Long>> hostsMap = new HashMap<>();
        hosts.forEach(h -> hostsMap.computeIfAbsent(h.getHostname(), k -> new ArrayList<Long>()).add(h.getAccessDelay()));
        List<Map.Entry<String, List<Long>>> duplicates = hostsMap.entrySet().stream()
                .filter(kv -> kv.getValue().size() > 1)
                .map(e -> pair(e.getKey(), (List<Long>)e.getValue()))
                .collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            throw new RuntimeException(String.format("Duplicate entries found for hosts: %s", duplicates));
        }
        return Collections.unmodifiableSet(
                hostsMap.entrySet().stream().map(e -> Host.of(e.getKey(), e.getValue().get(0))).collect(Collectors.toSet()));
    }

    public void run() throws SuspendExecution {
        for (URI url : seedUrls) {
            collection.add(url);
        }
        this.processors.forEach(p -> p.start());
        this.processors.forEach(p -> p.join());
    }

    public static Crawler of(Stream<Host> hosts, Stream<URI> seedUrls, UrlAction action, int processorsPerHost) throws SuspendExecution {
        return new Crawler(hostsToSet(hosts), seedUrls, action, processorsPerHost);
    }

    public static Crawler of(Stream<Host> hosts, UrlAction action, int processorsPerHost) throws SuspendExecution {
        Set<Host> hostSet = hostsToSet(hosts);
        Stream<URI> seedUrls = hostSet.stream().map(h -> uri(String.format("http://%s/", h.getHostname())));
        return new Crawler(hostSet, seedUrls, action, processorsPerHost);
    }

    @Value
    @RequiredArgsConstructor(staticName="of")
    public static class Host {
        String hostname;
        long accessDelay;
    }
}
