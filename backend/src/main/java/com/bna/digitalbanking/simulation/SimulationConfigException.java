package com.bna.digitalbanking.simulation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SimulationConfigException extends ResponseStatusException {
    public SimulationConfigException(HttpStatus status, String reason) {
        super(status, reason);
    }
}
