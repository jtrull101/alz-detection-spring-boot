package com.jtrull.alzdetection.Model;

import java.util.List;

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

@RestController
@RequestMapping(path = "api/v1/model")
public class ModelController {
    Logger logger = LoggerFactory.getLogger(ModelController.class);

    @Autowired
    private ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    // POST/CREATE mappings

    @PostMapping("/load")
    public Model loadModelFromFile(@RequestParam("file") MultipartFile file) throws Exception {
        logger.debug("entered loadModelFromFile for multipartfile: " + file);
        return this.modelService.loadModelFromFile(file);
    }

    // GET mappings

    @GetMapping("/{modelId}")
    public Model getModelById(@PathVariable Long modelId) {
        return this.modelService.getModelById(modelId);
    }

    @GetMapping("/all")
    public List<Model> getAllModels() {
        return this.modelService.getAllModels();
    }

    // DELETE mappings

    @DeleteMapping("/delete/{modelId}")
    public boolean deleteModelById(@PathVariable Long modelId) {
        return this.modelService.deleteModelById(modelId);
    }

    @DeleteMapping("/delete/all")
    public boolean deleteAllModels() {
        return this.modelService.deleteAllModels();
    }
}
