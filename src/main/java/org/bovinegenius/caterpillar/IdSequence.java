package org.bovinegenius.caterpillar;

import java.util.concurrent.atomic.AtomicLong;

public class IdSequence {
    private final AtomicLong idCounter = new AtomicLong();

    public String next() {
        return String.valueOf(idCounter.getAndIncrement());
    }
}
