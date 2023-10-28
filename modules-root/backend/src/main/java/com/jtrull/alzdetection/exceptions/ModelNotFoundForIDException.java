package com.jtrull.alzdetection.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class ModelNotFoundForIDException extends HttpClientErrorException{
    public static final String MESSAGE = "Unable to find model with Id: ";

    public ModelNotFoundForIDException(Long modelId) {
        super(HttpStatusCode.valueOf(404), MESSAGE + modelId);
    }


}
