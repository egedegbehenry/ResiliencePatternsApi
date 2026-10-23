package com.henry.resiliencepatternsapi.service;

import com.henry.resiliencepatternsapi.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Service;

@Service
public class SimulatedDownstreamService {

    public String call(boolean fail, int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DownstreamUnavailableException("Simulated downstream call was interrupted", exception);
        }
        return call(fail);
    }

    public String call(boolean fail) {
        if (fail) {
            throw new DownstreamUnavailableException();
        }
        return "Simulated downstream service responded successfully";
    }
}
