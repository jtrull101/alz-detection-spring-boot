package com.jtrull.alzdetection.exceptions.generic;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class UnexpectedDeleteErrException extends HttpClientErrorException{
    public static final String MESSAGE = "Encountered unexpected error during delete process: ";
    public static final HttpStatusCode CODE = HttpStatus.NOT_ACCEPTABLE;
    private List<String> details;
    
    public UnexpectedDeleteErrException(Exception ex, List<String> details) {
        super(CODE, MESSAGE + ex);
        this.details = details;
    }

    public UnexpectedDeleteErrException(Exception ex, String... details) {
        super(CODE, MESSAGE + ex);
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
