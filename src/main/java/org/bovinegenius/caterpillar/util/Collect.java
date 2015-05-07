package org.bovinegenius.caterpillar.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Collect {
    @SuppressWarnings("unchecked")
    public static <T> List<T> list(final T... ts) {
        return Collections.unmodifiableList(Arrays.stream(ts).collect(Collectors.toList()));
    }
}
