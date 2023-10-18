package com.jtrull.alzdetection.Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

    private final Path root = Paths.get("model");

    @Autowired
    private ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
        try {
            Files.createDirectories(this.root);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    // POST/CREATE mappings

    @PostMapping("/load")
    public Model loadModelFromFile(@RequestParam("file") MultipartFile file) throws Exception {
        Path destinationFile;
        try {
            if (file.isEmpty()) {
                throw new Exception("Failed to store empty file.");
            }
            destinationFile = this.root.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.root.toAbsolutePath())) {
                throw new Exception("Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new Exception("Failed to store file.", e);
        }
        
        return this.modelService.loadModelFromFile(destinationFile.toFile());
    }

    @PostMapping("/load/default")
    public Model loadDefaultModel() {
        return this.modelService.loadDefaultModel();
    }

    @PostMapping("/load/{modelId}")
    public Model loadModelById(@PathVariable Long modelId) {
        return this.modelService.loadModelById(modelId);
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


}
