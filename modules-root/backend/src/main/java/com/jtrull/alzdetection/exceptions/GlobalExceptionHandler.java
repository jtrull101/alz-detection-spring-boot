package com.jtrull.alzdetection.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;
import com.jtrull.alzdetection.exceptions.generic.UnexpectedDeleteErrException;
import com.jtrull.alzdetection.exceptions.model.InvalidModelFileException;
import com.jtrull.alzdetection.exceptions.model.ModelNotFoundException;
import com.jtrull.alzdetection.exceptions.predictions.InvalidImpairmentCategoryException;
import com.jtrull.alzdetection.exceptions.predictions.PredictionFailureException;
import com.jtrull.alzdetection.exceptions.predictions.PredictionNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    /*
     * Model
     */

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleModelNotFoundException(ModelNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }

    @ExceptionHandler(InvalidModelFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModelFileException(InvalidModelFileException ex) {
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(new ErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }
  
    /*
     * Predictions
     */

    @ExceptionHandler(PredictionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePredictionNotFoundException(PredictionNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }

    @ExceptionHandler(PredictionFailureException.class)
    public ResponseEntity<ErrorResponse> handlePredictionFailureException(PredictionFailureException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }

    @ExceptionHandler(InvalidImpairmentCategoryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImpairmentCategoryException(InvalidImpairmentCategoryException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }

    /*
     * Generic 
     */

    @ExceptionHandler(FailedRequirementException.class)
    public ResponseEntity<ErrorResponse> handleFailedRequirementException(FailedRequirementException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ErrorResponse(HttpStatus.NOT_ACCEPTABLE.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }

    @ExceptionHandler(UnexpectedDeleteErrException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedDeleteErrException(UnexpectedDeleteErrException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ErrorResponse(HttpStatus.NOT_ACCEPTABLE.value(), 
                ex.getMessage(), 
                ex.getDetails()));
    }
}
