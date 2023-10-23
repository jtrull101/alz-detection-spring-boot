package com.jtrull.alzdetection.Image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping(path = "api/v1/model/{modelId}/predict")
@CrossOrigin(origins = "http://localhost:4200")
public class ImageController {
    Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // POST mappings
    //  adding new prediction from image

    @PostMapping()
    public ImagePrediction runPredictionForImage(@RequestParam("image") MultipartFile file, @PathVariable Long modelId) {
        return this.imageService.runPredictionForImage(file, modelId) ;
    }

    // GET mappings
    //  using existing random /test endpoints

    @GetMapping("/random")
    public ImagePrediction runPredictionForRandomImage(@PathVariable Long modelId) { 
        return this.imageService.runPredictionForRandomImage(modelId);
    }

    @GetMapping("/random/{impairment}")
    public ImagePrediction runPredictionForRandomImage(@PathVariable String impairment, @PathVariable Long modelId) {
        return this.imageService.runPredictionForRandomFromImpairmentCategory(impairment, modelId);
    }

    @GetMapping("")
    public ImagePrediction runGetPrediction(@RequestParam(value="id") long predictionId, @PathVariable Long modelId) {
        return this.imageService.runGetPrediction(predictionId, modelId);
    }

    // DELETE mappings

    @DeleteMapping("/delete")
    public boolean runDeletePrediction(@RequestParam(value="id") long predictionId, @PathVariable Long modelId) {
        return this.imageService.runDeletePrediction(predictionId, modelId);
    }
}
