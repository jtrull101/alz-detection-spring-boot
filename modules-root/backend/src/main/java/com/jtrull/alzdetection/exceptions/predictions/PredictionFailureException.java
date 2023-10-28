package com.jtrull.alzdetection.exceptions.predictions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

public class PredictionFailureException extends HttpClientErrorException{
    public static final String MESSAGE = "Something has gone wrong for the prediction on desired input! ";
    public static final HttpStatusCode CODE = HttpStatus.BAD_REQUEST;
    private List<String> details;
    
    public PredictionFailureException(File file, List<String> details) {
        super(CODE, MESSAGE + file);
        this.details = details;
        this.details.add(file.toString());
    }
    public PredictionFailureException(File file, String... details) {
        super(CODE, MESSAGE + file);
        this.details = new ArrayList<>(Arrays.asList(details));
        this.details.add(file.toString());
    }
    public PredictionFailureException(MultipartFile file, String... details) {
        super(CODE, MESSAGE + file);
        this.details = new ArrayList<>(Arrays.asList(details));
        this.details.add(file.toString());
    }
    public PredictionFailureException(List<File> files, String... details) {
        super(CODE, MESSAGE + files.stream()
            .map(n -> String.valueOf(n))
            .collect(Collectors.joining("-", "{", "}")));
        this.details = new ArrayList<>(Arrays.asList(details));
        this.details.add(files.toString());
        this.details.add(String.valueOf(files.size()));
        
    }

    public List<String> getDetails() {
        return details;
    }
}