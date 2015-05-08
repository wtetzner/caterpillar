package org.bovinegenius.caterpillar;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class DelaySequence {
    private final long delay;
    private final AtomicReference<Instant> latest;

    private DelaySequence(Instant start, long delay) {
        this.delay = delay;
        this.latest = new AtomicReference<Instant>(start);
    }

    public Instant next() {
        return latest.updateAndGet(old -> old.plusMillis(this.delay));
    }

    public static DelaySequence of(long delay, Instant latest) {
        return new DelaySequence(latest, delay);
    }

    public static DelaySequence of(long delay) {
        return DelaySequence.of(delay, Instant.now().minusMillis(delay));
    }
}
