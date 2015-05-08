package org.bovinegenius.caterpillar;

import static org.bovinegenius.caterpillar.util.Collect.list;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName="of")
public class Host {
    String hostname;
    long accessDelay;
    List<String> initialPaths;

    public static Host host(String hostname, long accessDelay, String... initialPaths) {
        List<String> paths = (initialPaths == null || initialPaths.length == 0) ? list("/") : Arrays.asList(initialPaths);
        return Host.of(hostname, accessDelay, paths);
    }
}
