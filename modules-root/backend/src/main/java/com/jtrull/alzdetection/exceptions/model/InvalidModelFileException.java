package com.jtrull.alzdetection.exceptions.model;

import java.io.File;
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
        super(CODE, MESSAGE);
        this.details = details;
    }
    public InvalidModelFileException(MultipartFile file, String... details) {
        super(CODE, MESSAGE);
        this.details = new ArrayList<>(Arrays.asList(details));
    }
    public InvalidModelFileException(File file, String... details) {
        super(CODE, MESSAGE);
        this.details = new ArrayList<>(Arrays.asList(details));
        this.details.add(file.toString().contains(".png") ? "Unable to read seaborn plot from file" : 
            "Unable to read properties from file");
    }
    public InvalidModelFileException(Criteria<Image, Classifications> criteria, String... details) {
        super(CODE, MESSAGE);
        this.details = new ArrayList<>(Arrays.asList(details));
        String[] split = criteria.toString().split("\n");
        for (String s : Arrays.asList(split)){
            this.details.add(s.replace("\t", "  "));
        }
    }

    public List<String> getDetails() {
        return details;
    }
}
