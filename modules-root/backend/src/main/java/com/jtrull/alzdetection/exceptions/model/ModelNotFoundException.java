package com.jtrull.alzdetection.exceptions.model;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class ModelNotFoundException extends HttpClientErrorException{

    public static final String MESSAGE = "Unable to find model! ";
    public static final HttpStatusCode CODE = HttpStatus.NOT_FOUND;
    private List<String> details;
    
    public ModelNotFoundException(Long modelId, List<String> details) {
        super(CODE, MESSAGE + "search by ID for '" + modelId + "'");
        this.details = details;
    }

    public ModelNotFoundException(Long modelId, String... details) {
        super(CODE, MESSAGE + "search by ID for '" + modelId + "'");
        this.details = Arrays.asList(details);
    }

    public ModelNotFoundException(String filename, String... details) {
        super(CODE, MESSAGE + "search by file by filename '" + filename + "'");
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
