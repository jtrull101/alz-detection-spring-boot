package com.jtrull.alzdetection.Model;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.tensorflow.SavedModelBundle;

@Service
public class ModelService {

    private final String DEFAULT_MODEL_PATH = "model/";
    private final String SAVED_MODEL_EXTENSION = ".pb";

    private final ModelRepository modelRepository;
    public ModelService(ModelRepository repository) {
        this.modelRepository = repository;
    }

    public Model loadModelFromFile(File file) {
        Long id = generateIdFromPath(file.getAbsolutePath());
        String destinationDir = DEFAULT_MODEL_PATH + id;
       
        // Take file and move to its own subdir off of DEFAULT_MODEL_PATH
       

        Optional<File> fileOpt = getSavedModelInResourcesDir(destinationDir);
        if (fileOpt.isEmpty()) {
            // unable to find default model in expected dir
            assert false;
        }

        Model m = createModelFromFilepath(fileOpt, id);
        return modelRepository.save(m);
    }

    public Model loadDefaultModel() {
        Optional<File> fileOpt = getSavedModelInResourcesDir(DEFAULT_MODEL_PATH);
        if (fileOpt.isEmpty()) {
            // unable to find default model in expected dir
            assert false;
        }

        Model m = createModelFromFilepath(fileOpt);
        return modelRepository.save(m);
    }


    public Model loadModelById(Long modelId) {
        return null;
    }

    public Model getModelById(Long modelId) {
        return modelRepository.findById(modelId).get();
    }

    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    public boolean deleteModelById(Long modelId) {
        try{
            modelRepository.deleteById(modelId);
        }catch(Exception e) {
            return false;
        }
        return true;
    }

    public SavedModelBundle loadModelIntoTensorflow(Model m) {
        SavedModelBundle bundle = SavedModelBundle.load(m.getFilepath());
        return bundle;
    }


     private Model createModelFromFilepath(Optional<File> directory) {
        return createModelFromFilepath(directory, null);
    }

    private Long generateIdFromPath(String path) {
        return UUID.nameUUIDFromBytes(path.getBytes()).getMostSignificantBits();
    }

    private Model createModelFromFilepath(Optional<File> directory, Long desiredId) {
        String filePath = directory.get().getParent();
        if (desiredId == null) desiredId = generateIdFromPath(filePath);
        Model m = new Model(desiredId, filePath);
        return m;
    }

    private Optional<File> getSavedModelInResourcesDir(String path) {
        ClassLoader cl = getClass().getClassLoader();
        URL resource = cl.getResource(path);
        Optional<File> fileOpt = Optional.empty();
        try {
            fileOpt = Files.walk(Paths.get(resource.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(SAVED_MODEL_EXTENSION))
                    .map(x -> x.toFile())
                    .findAny();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return fileOpt;
    }
    
}
