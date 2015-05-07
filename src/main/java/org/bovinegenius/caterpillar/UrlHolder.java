package org.bovinegenius.caterpillar;

import java.net.URI;

import lombok.Value;

@Value
public class UrlHolder {
    URI url;
    boolean end;
}
