package com.jtrull.alzdetection.exceptions.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class InvalidModelConfigurationException extends HttpClientErrorException{

    public static final String MESSAGE = "Error encountered while processing input model! ";
    public static final HttpStatusCode CODE = HttpStatus.BAD_REQUEST;
    private List<String> details = new ArrayList<>();
    
    public InvalidModelConfigurationException(List<String> details) {
        super(CODE, MESSAGE);
        this.details = details;
    }
    public InvalidModelConfigurationException(String... details) {
        super(CODE, MESSAGE);
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
