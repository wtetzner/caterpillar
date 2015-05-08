package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.Host.host;
import static org.bovinegenius.caterpillar.util.Collect.list;

import java.time.Instant;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws Exception {
        Stream<Host> hosts = list(
                host("en.wikipedia.org", 0, "/"),
                host("bovinegenius.net", 0, "/"),
                host("news.ycombinator.com", 0, "/"))
                .stream();
        Crawler crawler = Crawler.of(hosts, (name, url, response) -> System.out.println(String.format("(%s) [%s] Processing %s", Instant.now(), name, url)), 50);
        System.out.println("Running...");
        crawler.run();
        System.out.println("Done.");
    }
}
