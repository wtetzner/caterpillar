package org.bovinegenius.caterpillar;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import lombok.Value;

import org.bovinegenius.caterpillar.util.Pair;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;


public class UrlMap<T> {
    private final ConcurrentHashMap<URI, List<URI>> mapping;

    private UrlMap() {
        this.mapping = new ConcurrentHashMap<>();
    }

    @Suspendable
    public UrlMapResult<T> getLinks(URI uri, LinkFetcher<T> fetcher) throws SuspendExecution {
        AtomicReference<Optional<T>> called = new AtomicReference<>(Optional.empty());
        List<URI> uris = this.mapping.computeIfAbsent(uri, new Function<URI, List<URI>>() {
            @Suspendable
            @Override public List<URI> apply(URI link) {
                Pair<T, List<URI>> fetchResult;
                try {
                    fetchResult = fetcher.fetchLinks(link);
                    called.set(Optional.of(fetchResult.getKey()));
                    return fetchResult.getValue();
                } catch (SuspendExecution e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        //        }; link -> {
        //            Pair<T,List<URI>> fetchResult = fetcher.fetchLinks(link);
        //            called.set(Optional.of(fetchResult.getKey()));
        //            return fetchResult.getValue();
        //        });
        return new UrlMapResult<>(uris, called.get(), !called.get().isPresent());
    }

    public static interface LinkFetcher<T> {
        public Pair<T, List<URI>> fetchLinks(URI uri) throws SuspendExecution;
    }

    public static <T> UrlMap<T> of() {
        return new UrlMap<T>();
    }

    @Value
    public static class UrlMapResult<T> {
        List<URI> links;
        Optional<T> data;
        boolean known;
    }
}
