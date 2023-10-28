package com.jtrull.alzdetection.exceptions.predictions;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class PredictionNotFoundException extends HttpClientErrorException{

    public static final String MESSAGE = "Unable to find prediction! ";
    public static final HttpStatusCode CODE = HttpStatus.NOT_FOUND;
    private List<String> details;
    
    public PredictionNotFoundException(Long predictionId, List<String> details) {
        super(CODE, MESSAGE + "search by ID for '" + predictionId + "'");
        this.details = details;
    }

    public PredictionNotFoundException(Long predictionId, String... details) {
        super(CODE, MESSAGE + "search by ID for '" + predictionId + "'");
        this.details = Arrays.asList(details);
    }

    public PredictionNotFoundException(String filename, String... details) {
        super(CODE, MESSAGE + "search by file by filename '" + filename + "'");
        this.details = Arrays.asList(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
