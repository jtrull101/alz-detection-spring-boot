package com.jtrull.alzdetection.Image;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "api/v1/model/{modelID}/predict")
public class ImageController {
    Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // POST mappings
    //  adding new prediction from image

    @PostMapping("/")
    public ImagePrediction runPredictionForImage(@RequestParam("image") MultipartFile file, HttpServletRequest request) {
        return this.imageService.runPredictionForImage(file, getModelNumFromRequest(request)) ;
    }

    // GET mappings
    //  using existing random /test endpoints
    @GetMapping("/random")
    public ImagePrediction runPredictionForRandomImage(HttpServletRequest request) { 
        return this.imageService.runPredictionForRandomImage(getModelNumFromRequest(request));
    }

    @GetMapping("/{impairment}")
    public ImagePrediction runPredictionForRandomImage(@PathVariable String impairment, HttpServletRequest request) {
        return this.imageService.runPredictionForRandomFromImpairmentCategory(impairment, getModelNumFromRequest(request));
    }

    // DELETE mappings
    @DeleteMapping("/{id}")
    public boolean runDeletePrediction(@PathVariable long id, HttpServletRequest request) {
        try {
            return this.imageService.runDeletePrediction(id, getModelNumFromRequest(request));
        } catch (Exception e){
            return false;
        }
    }

    /**
     * 
     * @param request
     * @return
     * @throws Exception
     */
    public Long getModelNumFromRequest(HttpServletRequest request) {
        Pattern p = Pattern.compile("(?<=\\/model\\/)(.*)(?=\\/predict)");
        Matcher m = p.matcher(request.getRequestURI());
        long modelNum = -1;
        if (m.find()) {
            modelNum = Long.valueOf(m.group(0));
        }
        if (modelNum < 0) throw new RuntimeException("Unable to find model number in request: " + request);
        return modelNum;
    }
}
