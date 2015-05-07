package org.bovinegenius.caterpillar.util;

import java.util.Map;

import lombok.Value;

@Value
public class Pair<K,V> implements Map.Entry<K,V> {
    K key;
    V value;

    @Override
    public V setValue(Object value) {
        throw new UnsupportedOperationException("setValue() is unsupported in Pair.");
    }

    @Override
    public String toString() {
        return String.format("%s => %s", key, value);
    }

    public static <K,V> Pair<K,V> pair(K key, V value) {
        return new Pair<K,V>(key, value);
    }
}
