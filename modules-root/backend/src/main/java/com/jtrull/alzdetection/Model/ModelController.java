package com.jtrull.alzdetection.Model;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.Image.ImageService;

@RestController
@RequestMapping(path = "api/v1/model")
@CrossOrigin(origins = "http://localhost:4200")
public class ModelController {
    Logger logger = LoggerFactory.getLogger(ModelController.class);

    @Autowired private ModelService modelService;
    @Autowired private ImageService imageService;

    public ModelController(ModelService modelService, ImageService imageService) {
        this.modelService = modelService;
        this.imageService = imageService;
    }

    // POST/CREATE mappings

    @PostMapping("/load")
    public Model loadModelFromFile(@RequestParam("file") MultipartFile file) throws Exception {
        Model m = this.modelService.loadModelFromFile(file);
        try {
            logger.debug("running random prediction on new model: " + m.getId());
            this.imageService.runPredictionForRandomImage(m.getId());

        } catch (Exception e) {
            logger.warn("Unable to use model with ID: " + m.getId() + ", removing from inventory");
            this.modelService.deleteModelById(m.getId());
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), 
                    "Error while assessing new model: " + e);
        }

        return m;
    }

    // GET mappings

    @GetMapping("")
    public Model getModelById(@RequestParam(value="id") Long modelId) {
        return this.modelService.getModelById(modelId);
    }

    @GetMapping("/all")
    public List<Model> getAllModels() {
        return this.modelService.getAllModels();
    }

    // DELETE mappings

    @DeleteMapping("/delete")
    public boolean deleteModelById(@RequestParam(value="id") Long modelId) {
        return this.modelService.deleteModelById(modelId);
    }

    @DeleteMapping("/delete/all")
    public boolean deleteAllModels() {
        return this.modelService.deleteAllModels();
    }
}
