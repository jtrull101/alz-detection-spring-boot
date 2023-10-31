package com.jtrull.alzdetection.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;
import com.jtrull.alzdetection.exceptions.model.InvalidModelFileException;
import com.jtrull.alzdetection.exceptions.model.ModelNotFoundException;
import com.jtrull.alzdetection.image.ImageService;


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
    public ResponseEntity<Model> loadModelFromFile(@RequestParam("model") MultipartFile model) {
        Model m = this.modelService.loadModelFromFile(model);

        // Run a random prediction on the new model to assert it is a valid model
        try {
            logger.debug("running random prediction on new model: " + m.getId());
            this.imageService.runPredictionForRandomImage(m.getId());

        } catch (Exception e) {
            logger.warn("Unable to use model with ID: " + m.getId() + ", removing from inventory");
            this.modelService.deleteModelById(m.getId());
            throw new InvalidModelFileException(model, "Error while assessing new model: " + e);
        }

        return ResponseEntity.ok(m);
    }

    @PostMapping("/load/details")
    public ResponseEntity<Model> loadModelFromFile(@RequestParam(value="id") Long modelId, @RequestParam("plot") Optional<MultipartFile> plot, @RequestParam("properties") Optional<MultipartFile> properties) {
        Model m = this.modelService.getModelById(modelId);
        Path path = Paths.get(m.getFilepath());
        
        // Copy plot to same directory if present
        if (plot.isPresent()) {
            Path destinationFile = path.resolve(Paths.get(plot.get().getOriginalFilename())).normalize().toAbsolutePath();
            try (InputStream inputStream = plot.get().getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FailedRequirementException(e,  "Unable to move seaborn plot file '" + plot.get().getName() + "' to destination: " + destinationFile);
            }
        }

        // Copy properties to same directory if present
        if (properties.isPresent()) {
            Path destinationFile = path.resolve(Paths.get(properties.get().getOriginalFilename())).normalize().toAbsolutePath();
            try (InputStream inputStream = properties.get().getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FailedRequirementException(e,  "Unable to move properties file '" + properties.get().getName() + "' to destination: " + destinationFile);
            }
        }

        m = m.findPlotForKey();
        m = m.findPropertiesForKey();
        this.modelService.updateModelInRepository(m);
        return ResponseEntity.ok(m);
    }

    // GET mappings

    @GetMapping("")
    public ResponseEntity<Model> getModelById(@RequestParam(value="id") Long modelId) {
        return ResponseEntity.ok(this.modelService.getModelById(modelId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Model>> getAllModels() {
        return ResponseEntity.ok(this.modelService.getAllModels());
    }

    @GetMapping(value="/plot", produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPlotById(@RequestParam(value="id") Long modelId) {
        String plotPath = this.modelService.getModelById(modelId).getSeabornPlotPath();
        if (plotPath == null) {
            throw new ModelNotFoundException(modelId, "Unable to find seaborn plot for specified model Id");
        }
        try {
            return ResponseEntity.ok(Files.readAllBytes(Paths.get(plotPath)));
        } catch (IOException e) {
            throw new InvalidModelFileException(new File(plotPath), "IO exception message: " + e.getMessage());
        }
    }

    // DELETE mappings

    @DeleteMapping("/delete")
    public ResponseEntity<Boolean> deleteModelById(@RequestParam(value="id") Long modelId) {
        return ResponseEntity.ok(this.modelService.deleteModelById(modelId));
    }

    @DeleteMapping("/delete/all")
    public ResponseEntity<Boolean> deleteAllModels() {
        return ResponseEntity.ok(this.modelService.deleteAllModels());
    }
}
