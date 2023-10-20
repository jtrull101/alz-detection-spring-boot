package com.jtrull.alzdetection.Model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import ai.djl.Application;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import jakarta.annotation.PostConstruct;

@Service
public class ModelService {
    Logger logger = LoggerFactory.getLogger(ModelService.class);

    private static final Path root = Paths.get("model");
    private static final String SAVED_MODEL_ARCHIVE_EXTENSION = ".zip";
    public static final String DEFAULT_MODEL_NAME = "default_model" + SAVED_MODEL_ARCHIVE_EXTENSION;

    private HashMap<Long, Criteria<Image, Classifications>> inMemoryModels = new HashMap<>();

    private final ModelRepository modelRepository;
    public ModelService(ModelRepository repository) {
        this.modelRepository = repository;
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Files.createDirectories(root);
            logger.info("created path: " + path);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }

        // load default model into DB
        try {
            loadDefaultModel();
        } catch (Exception e) {
            logger.error("Unable to load default model: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        initInMemoryModels();
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
                throw new RuntimeException("Failed to store empty file.");
            }
            if (!FilenameUtils.getExtension(file.getOriginalFilename()).equals("zip")) {
                throw new RuntimeException("Unable to load model from file that is not a .zip");
            }
            String sourcePath = ModelService.class.getResource("/").getPath() + root;
            Path newPath = Paths.get(sourcePath + "/" + file.getName().hashCode());
            Files.createDirectories(newPath);


            // Take file and move to its own subdir off of model path
            destinationFile = newPath.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }


        File resource = findModelResourceInDir(destinationFile.toFile().getName());
        Model m = createModelFromFilepath(resource);
        return modelRepository.save(m);
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public Model loadDefaultModel() throws Exception {
        File resource = findModelResourceInDir(DEFAULT_MODEL_NAME);
        Model m = createModelFromFilepath(resource);
        return modelRepository.save(m);
    }

    /**
     * 
     * @param directory
     * @return
     */
    public File findModelResourceInDir(String filename) {
        Optional<File> resource = getSavedModelInResourcesDir(filename);
        if (resource.isEmpty()) {
            throw new RuntimeException("Unable to find model for file: " + filename + " in directory: " + root);
        }
        File f = resource.get();
        logger.info("returning found file: " + f);
        return f;
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
        if (modelId == 1) {
            logger.debug("unable to delete default model with id: " + modelId);
            return false;
        }
        try{
            logger.debug("removing model " + modelId + " from in memory models");
            inMemoryModels.remove(modelId);
            logger.debug("deleting model " + modelId + " model from database");
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
    public Criteria<Image, Classifications> loadModelIntoTensorflow(Model m) {
        Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                .addTransform(a -> NDImageUtils.resize(a, 128, 128).div(225.0f))
                .optSynset(ImpairmentEnum.asStrings())
                .build();

        Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class)
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .optEngine("TensorFlow")
                .optModelPath(Paths.get(m.getFilepath() + "/" + m.getName()))
                .optModelName(m.getName())
                .optTranslator(translator)
                .optProgress(new ProgressBar())
                .build();

        return criteria;
    }


    /**
     * 
     * @param directory
     * @param desiredId
     * @return
     */
    private Model createModelFromFilepath(File directory) {
        String filePath = directory.getParent();
        Model m = new Model(filePath, directory.getName());
        return m;
    }


    private Optional<File> returnFileFromPath(String filename, String path) {
        Optional<File> fileOpt;
        try {
            fileOpt = Files.walk(Paths.get(path))
                        .filter(Files::isRegularFile)
                        .filter(r -> r.toFile().getName().equals(filename))
                        .filter(r -> r.getFileName().toString().contains(SAVED_MODEL_ARCHIVE_EXTENSION))
                        .map(x -> x.toFile())
                        .findFirst();
            return fileOpt;

        } catch (IOException e) {
            throw new RuntimeException("Error finding model with name " + filename + " in directory: " + path);
        }
    }
    /**
     * 
     * @param path
     * @return
     */
    public Optional<File> getSavedModelInResourcesDir(String filename) {
        String path = getClass().getResource("/").getPath() + root;
        logger.info("Running getResource for path: " + path);
        try {
            Optional<File> defaultModelInBasePath = returnFileFromPath(filename, path);
            if (defaultModelInBasePath.isPresent()) return defaultModelInBasePath;

            // recurisvely find all files
            Optional<Optional<File>> f = Files.walk(Paths.get(path))
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .map(x -> returnFileFromPath(filename, x.getAbsolutePath()))
                    .findFirst();
                
            if  (f.isPresent() && f.get().isPresent()) {
                return f.get();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error finding model with name " + filename + " in directory: " + path);
        }

        return Optional.empty();
    }

    

     /**
     * 
     * @param m
     * @return
     */
    public HashMap<Long, Criteria<Image, Classifications>> addModelToInMemoryModels(Model m) {
        Criteria<Image, Classifications> criteria = loadModelIntoTensorflow(m);
        inMemoryModels.put(m.getId(), criteria);
        return inMemoryModels;
    }

    /**
     * 
     */
    public void initInMemoryModels() {
        List<Model> models = modelRepository.findAll();
        for (Model m : models) {
            logger.debug("loading model into memory: " + m);
            addModelToInMemoryModels(m);
        }
    }

    /**
     * 
     * @return
     */
    public HashMap<Long, Criteria<Image, Classifications>> getInMemoryModels() {
        return this.inMemoryModels;
    }
    
}
