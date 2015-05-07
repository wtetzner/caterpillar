package org.bovinegenius.caterpillar;

import java.net.URI;

import co.paralleluniverse.fibers.SuspendExecution;

public interface UrlSink {
    public void add(URI url) throws SuspendExecution;
}
