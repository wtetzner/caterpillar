package org.bovinegenius.caterpillar;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName="of")
public class DelayedUrl implements Delayed {
    Instant endTime;
    URI url;

    private long getDiff() {
        long currentTime = Instant.now().toEpochMilli();
        return endTime.toEpochMilli() - currentTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(getDiff(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == null) {
            return 1;
        } else {
            long delay = getDelay(TimeUnit.MILLISECONDS);
            long otherMillis = o.getDelay(TimeUnit.MILLISECONDS);
            if (delay < otherMillis) {
                return -1;
            } else if (delay == otherMillis) {
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s@%s", this.url, this.endTime);
    }
}
