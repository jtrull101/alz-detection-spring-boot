package com.jtrull.alzdetection.general;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jtrull.alzdetection.exceptions.generic.UnrecognizedEndpointException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
@CrossOrigin(origins = "http://localhost:4200")
public class BaseController implements ErrorController {
    Logger logger = LoggerFactory.getLogger(BaseController.class);

    private static final String PATH = "/error";

    public BaseController() {
    }

    @RequestMapping(PATH)
    public String unrecognizedEndpoint(HttpServletRequest request) {
        throw new UnrecognizedEndpointException();
    }
}
