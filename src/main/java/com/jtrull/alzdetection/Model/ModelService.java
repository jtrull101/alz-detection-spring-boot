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
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    /**
     * Invoked after the constructor, perform initialization operations including:
     * 1) Creating the directory where models will be stored once uploaded,
     * 2) Loading the default model into the database
     * 3) Loading each model in the database into the in-memory Tensorflow objects
     */
    @PostConstruct
    public void init() { 

        // create directories where uploaded models are stored
        try {
            Files.createDirectories(Paths.get(returnModelPath()));
        } catch (Exception ex) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(409), "Could not create the directory where uploaded model files will be stored.");
        }

        // load default model into DB
        loadDefaultModel();
        
        // init the in-memory models
        initInMemoryModels();
    }

    /**
     * Return the path where models will be stored, represented in some way by /resource/model
     * 
     * @return
     */
    public String returnModelPath() {
        return getClass().getResource("/").getPath() + root;
    }

    /**
     * Given a .zip MultipartFile as input, move this file to the /resources/model/{hash(file.getName())} and load the Model into the database.
     * 
     * @param file
     * @return Model - created from this input MultipartFile
     */
    public Model loadModelFromFile(MultipartFile file) {
        logger.trace("entered loadModelFromFile for file: " + file);
        Path destinationFile;

        // Error checks
        if (file.isEmpty()) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Failed to store empty file.");
        }
        if (!FilenameUtils.getExtension(file.getOriginalFilename()).equals("zip")) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(415), "Unable to load model from file that is not a .zip");
        }

        // Create directory to hold new model, constructed of a hash of the model's name.
        //      This assumption implies unique model zip names
        Path newPath = Paths.get(returnModelPath() + "/" + file.getName().hashCode());
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(406), 
                    "Unable to create new directory to store model at: " + newPath + " message  = " + e.getMessage());
        }

        // Copy the model .zip to the resources directory
        destinationFile = newPath.resolve(Paths.get(file.getOriginalFilename())).normalize().toAbsolutePath();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(406), 
                    "Unable to move modle zip file '" + file.getName() + "' to destination: " + destinationFile + " message  = " + e.getMessage());
        }

        // Find the model.zip in its subdirectory in the resources directory, 
        //      create a Model object and save to the database
        File resource = findModelResourceInDir(destinationFile.toFile().getName());
        Model m = createModelFromFilepath(resource);
        addModelToInMemoryModels(m);
        
        synchronized (modelRepository) {
            return modelRepository.save(m);
        }
    }

    /**
     * Load the default model packaged with this jar into the database.
     * 
     * @return Model - loaded default Model
     */
    public Model loadDefaultModel() {
        File resource = findModelResourceInDir(DEFAULT_MODEL_NAME);
        Model m = createModelFromFilepath(resource);
        return modelRepository.save(m);
    }

    /**
     * Given a model's filename, attempt to find the file in the expected directory where models are stored.
     * 
     * @param filename
     * @return File - corresponding to this model if found
     */
    public File findModelResourceInDir(String filename) {
        Optional<File> resource = getSavedModelInResourcesDir(filename);
        if (resource.isEmpty()) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model for filename: " + filename);
        }
        File f = resource.get();
        logger.trace("returning found file: " + f);
        return f;
    }

    /**
     * Get the model at the specified Id
     * 
     * @param modelId
     * @return
     */
    public Model getModelById(Long modelId) {
        Optional<Model> opt = modelRepository.findById(modelId);
        if (opt.isEmpty()) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model with Id: " + modelId);
        }
        return opt.get();
    }
    
    /**
     * Return a list of all current models in the database
     * 
     * @return
     */
    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    /**
     * Given a modelId, attempt to delete that model
     * 
     * @param modelId
     * @return
     */
    public boolean deleteModelById(Long modelId) {
        if (modelId == 1) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(403), "Unable to delete default model");
        }
        if (modelRepository.findById(modelId).isEmpty()){
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model with Id: " + modelId);
        }

        try {
            synchronized (inMemoryModels) {
                inMemoryModels.remove(modelId);
            }
            synchronized (modelRepository) {
                modelRepository.deleteById(modelId);
            }
        } catch(Exception e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(500), 
                "Error occurred during deletion of model with Id:" + modelId + ". message = " + e.getMessage());
        }
        return true;
    }
 
    /**
     * Delete all models currently existing in the database and in memory.
     * 
     * @return
     */
    public boolean deleteAllModels() {
        try {
            synchronized (inMemoryModels) {
                Set<Entry<Long, Criteria<Image, Classifications>>> entries = inMemoryModels.entrySet();
                entries.stream()
                    .filter(e -> e != null).filter(e -> e.getKey() != null)
                    .filter(e -> e.getKey() != 1)
                    .forEach(e -> inMemoryModels.remove(e.getKey()));
            }
            synchronized (modelRepository) {
                List<Model> models = modelRepository.findAll();
                models.stream()
                    .filter(m -> m.getId() != 1)
                    .forEach(m -> modelRepository.delete(m));
            }
        }catch(Exception e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(500), 
                "Error occurred during deletion of all models. message = " + e.getMessage());
        }
        return true;
    }

    /**
     * Load the Model object into a Translator and Criteria to be used with the Amazon Tensorflow Java API DJL.
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
     * Create a Model object representation from a directory.
     * 
     * @param directory
     * @return
     */
    private Model createModelFromFilepath(File directory) {
        return new Model(directory.getParent(), directory.getName());
    }

    /**
     * Given the specified path, attempt to find the file with the given filename and extension.
     * 
     * @param filename
     * @param path
     * @return
     */
    private Optional<File> returnFileFromPath(String filename, String path) {
        try {
            return Files.walk(Paths.get(path))
                        .filter(Files::isRegularFile)
                        .filter(r -> r.toFile().getName().equals(filename))
                        .map(x -> x.toFile())
                        .findFirst();

        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(409), 
                "Error while traversing path '" + path + "' to find model with name '" + filename + "'. message = " + e.getMessage());
        }
    }

    

    /**
     * Find a model with the specified filename in the resources/model/ directory.
     * 
     * @param filename
     * @return
     */
    public Optional<File> getSavedModelInResourcesDir(String filename) {
        String path = returnModelPath();
        logger.trace("Running getResource for path: " + path);
        try {
            Optional<File> defaultModelInBasePath = returnFileFromPath(filename, path);
            if (defaultModelInBasePath.isPresent()) return defaultModelInBasePath;

            // find all files
            Optional<Optional<File>> f = Files.walk(Paths.get(path))
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .map(x -> returnFileFromPath(filename, x.getAbsolutePath()))
                    .findFirst();
                
            if  (f.isPresent() && f.get().isPresent()) {
                return f.get();
            }

        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(409), 
                "Error while traversing path '" + path + "' to find model with name '" + filename + "'. message = " + e.getMessage());
        }
        return Optional.empty();
    }

    

     /**
     * Add a Model object to the map of current in-memory Criteria objects. The Amazon Tensorflow API can take a bit of time to load in models, 
     *  and these models once loaded into memory are not Serializable. Store all the required information to spin up a model in the database,
     *  the Model object, then load the models in the database into memory by creating Criteria objects.
     * 
     * @param m
     * @return
     */
    public HashMap<Long, Criteria<Image, Classifications>> addModelToInMemoryModels(Model m) {
        Criteria<Image, Classifications> criteria = loadModelIntoTensorflow(m);
        synchronized (inMemoryModels) {
            inMemoryModels.put(m.getId(), criteria);
        }
        return inMemoryModels;
    }

    /**
     * Initialize the in-memory models by finding all Models in the ModelRepository and adding in-memory representations of them.
     */
    public void initInMemoryModels() {
        modelRepository.findAll().stream().forEach(m -> addModelToInMemoryModels(m));
    }

    /**
     * Get all current in-memory models.
     * 
     * @return
     */
    public HashMap<Long, Criteria<Image, Classifications>> getInMemoryModels() {
        return this.inMemoryModels;
    }
    
}
