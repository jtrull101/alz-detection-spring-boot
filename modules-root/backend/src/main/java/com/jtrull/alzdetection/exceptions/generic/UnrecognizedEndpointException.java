package com.jtrull.alzdetection.exceptions.generic;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class UnrecognizedEndpointException extends HttpClientErrorException {
    public static final String MESSAGE = "Unable to access unrecognized endpoint";
    public static final HttpStatusCode CODE = HttpStatus.BAD_REQUEST;
    private List<String> details;
    
    public UnrecognizedEndpointException(List<String> details) {
        super(CODE, MESSAGE);
        this.details = details;
    }

    public UnrecognizedEndpointException(String... details) {
        super(CODE, MESSAGE);
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
