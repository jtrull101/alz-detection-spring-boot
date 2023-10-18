package com.jtrull.alzdetection.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.Model.ModelController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "api/v1/model/{modelID}/predict")
public class ImageController {
    Logger logger = LoggerFactory.getLogger(ModelController.class);

    private final Path root = Paths.get("images");

    @Autowired
    private ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
        try {
            Files.createDirectories(this.root);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    // POST mappings

    @PostMapping("/")
    public ImagePrediction runPredictionForImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        return this.imageService.runPredictionForImage(file, getModelNumFromRequest(request)) ;
    }

    @PostMapping("/random")
    public ImagePrediction runPredictionForRandomImage(HttpServletRequest request) throws Exception { 
        return this.imageService.runPredictionForRandomImage(getModelNumFromRequest(request));
    }

    @PostMapping("/{ImpairmentEnum}")
    public ImagePrediction runPredictionForRandomImage(@PathVariable String impairment, HttpServletRequest request) throws Exception {
        return this.imageService.runPredictionForRandomFromImpairmentCategory(impairment, getModelNumFromRequest(request));
    }

    /**
     * 
     * @param request
     * @return
     * @throws Exception
     */
    public Long getModelNumFromRequest(HttpServletRequest request) throws Exception {
        Pattern p = Pattern.compile("(?<=\\/model\\/)(.*)(?=\\/predict\\/random)");
        Matcher m = p.matcher(request.getRequestURI());
        long modelNum = -1;
        if (m.find()) {
            modelNum = Long.valueOf(m.group(0));
        }

        if (modelNum < 0) {
            throw new Exception("Unable to find model number in request: " + request);
        }
        return modelNum;
    }
}
