package com.jtrull.alzdetection.exceptions.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.Criteria;

public class InvalidModelFileException extends HttpClientErrorException{

    public static final String MESSAGE = "Unable to create model for input: ";
    public static final HttpStatusCode CODE = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    private List<String> details = new ArrayList<>();
    
    public InvalidModelFileException(MultipartFile file, List<String> details) {
        super(CODE, MESSAGE + file);
        this.details = details;
    }
    public InvalidModelFileException(MultipartFile file, String... details) {
        super(CODE, MESSAGE + file);
        this.details = Arrays.asList(details);
    }
    public InvalidModelFileException(Criteria<Image, Classifications> criteria, String... details) {
        super(CODE, MESSAGE + criteria);
        this.details = Arrays.asList(details);
    }

    //   public InvalidModelFileException(MultipartFile file) {
    //     super(CODE, MESSAGE + file);
    // }

    public List<String> getDetails() {
        return details;
    }
}
