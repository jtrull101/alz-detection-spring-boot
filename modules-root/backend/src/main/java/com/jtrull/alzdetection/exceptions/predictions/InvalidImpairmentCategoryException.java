package com.jtrull.alzdetection.exceptions.predictions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import com.jtrull.alzdetection.prediction.ImpairmentEnum;

public class InvalidImpairmentCategoryException extends HttpClientErrorException{
    public static final String MESSAGE = "Chosen category is not one of the specified Impairment categories: " + Arrays.asList(ImpairmentEnum.asStrings().toArray());
    public static final HttpStatusCode CODE = HttpStatus.BAD_REQUEST;
    private List<String> details;
    
    public InvalidImpairmentCategoryException(String value, List<String> details) {
        super(CODE, MESSAGE);
        this.details = details;
        this.details.add("chosen value: '" + value + "'");
    }

    public InvalidImpairmentCategoryException(String value, String... details) {
        super(CODE, MESSAGE);
        this.details = new ArrayList<>(Arrays.asList(details));
        this.details.add("chosen value: '" + value + "'");
    }

    public List<String> getDetails() {
        return details;
    }
}