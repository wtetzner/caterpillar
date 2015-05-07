package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.Collect.list;
import static org.bovinegenius.caterpillar.util.Pair.pair;

import java.util.stream.Stream;

import org.bovinegenius.caterpillar.Crawler.Host;

public class Main {
    public static void main(String[] args) throws Exception {
        Stream<Host> hosts = list(
                pair("en.wikipedia.org", 0))
                .stream()
                .map(p -> Host.of(p.getKey(), p.getValue()));
        Crawler crawler = Crawler.of(hosts, (url, response) -> System.out.println(String.format("Processing %s", url)), 12);
        System.out.println("Running...");
        crawler.run();
        System.out.println("Done.");
    }
}
