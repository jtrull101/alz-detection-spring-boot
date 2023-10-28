package com.jtrull.alzdetection.exceptions.generic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class FailedRequirementException extends HttpClientErrorException{

    public static final String MESSAGE = "Error creating one of the requirements for this app! ";
    public static final HttpStatusCode CODE = HttpStatus.NOT_ACCEPTABLE;
    private List<String> details = new ArrayList<>();
    
    public FailedRequirementException(Exception e, List<String> details) {
        super(CODE, MESSAGE + e);
        this.details = details;
    }
    public FailedRequirementException(Exception e, String... details) {
        super(CODE, MESSAGE + e);
        this.details = Arrays.asList(details);
    }
    public FailedRequirementException(Exception e, HttpStatusCode overrideCode, String... details) {
        super(overrideCode, MESSAGE + e);
        // using final CODE above means our test cannot accurately pull the CODE if referencing the exception statically
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    } 
}