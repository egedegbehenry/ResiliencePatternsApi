package com.henry.resiliencepatternsapi.exception;

public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException() {
        super("Simulated downstream service is unavailable");
    }
}
