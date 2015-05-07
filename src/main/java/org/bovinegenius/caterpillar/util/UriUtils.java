package org.bovinegenius.caterpillar.util;

import java.net.URI;

public class UriUtils {
    public static URI uri(String uri) {
        try {
            return new URI(uri);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
