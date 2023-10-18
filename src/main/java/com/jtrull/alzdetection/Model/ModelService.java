package com.jtrull.alzdetection.Model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;

@Service
public class ModelService {
    Logger logger = LoggerFactory.getLogger(ModelService.class);

    //TODO: consolidate this
    private final Path root = Paths.get("model");
    private final String DEFAULT_MODEL_PATH = "model/";
    private final String SAVED_MODEL_EXTENSION = ".pb";

    private final ModelRepository modelRepository;
    public ModelService(ModelRepository repository) {
        this.modelRepository = repository;
    }

    /**
     * 
     * @param file
     * @return
     * @throws Exception
     */
    public Model loadModelFromFile(MultipartFile file) throws Exception {
        logger.info("entered loadModelFromFile for file: " + file);
        Path destinationFile;
        try {
            if (file.isEmpty()) {
                throw new Exception("Failed to store empty file.");
            }
            Path newPath = Paths.get(root + "/"  + file.getName().hashCode());
            Files.createDirectories(newPath);
            destinationFile = newPath.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new Exception("Failed to store file.", e);
        }

        // Take file and move to its own subdir off of DEFAULT_MODEL_PATH
        File resource = findModelResourceInDir(destinationFile.toFile().getParent());
        Model m = createModelFromFilepath(resource);
        return modelRepository.save(m);
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public Model loadDefaultModel() throws Exception {
        File resource = findModelResourceInDir(DEFAULT_MODEL_PATH);
        Model m = createModelFromFilepath(resource);
        return modelRepository.save(m);
    }

    /**
     * 
     * @param directory
     * @return
     * @throws Exception
     */
    public File findModelResourceInDir(String directory) throws Exception {
         Optional<File> resource = getSavedModelInResourcesDir(DEFAULT_MODEL_PATH);
        if (resource.isEmpty()) {
            throw new Exception("Unable to find model for file: " + DEFAULT_MODEL_PATH);
        }
        return resource.get();
    }

    /**
     * 
     * @param modelId
     * @return
     */
    public Model getModelById(Long modelId) {
        return modelRepository.findById(modelId).get();
    }

    /**
     * 
     * @return
     */
    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    /**
     * 
     * @param modelId
     * @return
     */
    public boolean deleteModelById(Long modelId) {
        try{
            modelRepository.deleteById(modelId);
        }catch(Exception e) {
            return false;
        }
        return true;
    }
 
    /**
     * 
     * @return
     */
    public boolean deleteAllModels() {
        try{
            modelRepository.deleteAll();
        }catch(Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @param m
     * @return
     */
    public SavedModelBundle loadModelIntoTensorflow(Model m) {
        SavedModelBundle bundle = SavedModelBundle.load(m.getFilepath());
        return bundle;
    }


    /**
     * 
     * @param directory
     * @return
     */
    private Model createModelFromFilepath(File directory) {
        return createModelFromFilepath(directory, null);
    }

    /**
     * 
     * @param path
     * @return
     */
    private Long generateIdFromPath(String path) {
        return UUID.nameUUIDFromBytes(path.getBytes()).getMostSignificantBits();
    }

    /**
     * 
     * @param directory
     * @param desiredId
     * @return
     */
    private Model createModelFromFilepath(File directory, Long desiredId) {
        String filePath = directory.getParent();
        if (desiredId == null) desiredId = generateIdFromPath(filePath);
        Model m = new Model(desiredId, filePath);
        return m;
    }

    /**
     * 
     * @param path
     * @return
     */
    public Optional<File> getSavedModelInResourcesDir(String path) {
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
