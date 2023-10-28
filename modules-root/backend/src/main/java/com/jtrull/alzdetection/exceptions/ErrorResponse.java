package com.jtrull.alzdetection.exceptions;

import java.util.List;

public class ErrorResponse {
    private int statusCode;
    private String message;
    private List<String> details;
    private Throwable throwable;

    public ErrorResponse(int statusCode, String message, List<String> details, Throwable throwable) {
        this.statusCode = statusCode;
        this.message = message;
        this.details = details;
        this.throwable = throwable;
    }

    public ErrorResponse(int statusCode, String message, List<String> details) {
        this.statusCode = statusCode;
        this.message = message;
        this.details = details;
    }

    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public List<String> getDetails() {
        return details;
    }
    public void setDetails(List<String> details) {
        this.details = details;
    }
    public Throwable getThrowable() {
        return throwable;
    }
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

}
