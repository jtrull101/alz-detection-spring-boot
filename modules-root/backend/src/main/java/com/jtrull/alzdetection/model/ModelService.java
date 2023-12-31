package com.jtrull.alzdetection.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;
import com.jtrull.alzdetection.exceptions.generic.UnexpectedDeleteErrException;
import com.jtrull.alzdetection.exceptions.model.InvalidModelFileException;
import com.jtrull.alzdetection.exceptions.model.ModelNotFoundException;
import com.jtrull.alzdetection.general.Utils;
import com.jtrull.alzdetection.image.ImagePrediction;
import com.jtrull.alzdetection.image.ImageRepository;
import com.jtrull.alzdetection.prediction.ImpairmentEnum;

import ai.djl.Application;
import ai.djl.modality.Classifications;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.modality.cv.util.NDImageUtils;
// import ai.djl.ndarray.NDManager;
// import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
// import ai.djl.training.DefaultTrainingConfig;
// import ai.djl.training.EasyTrain;
// import ai.djl.training.Trainer;
// import ai.djl.training.evaluator.Accuracy;
// import ai.djl.training.listener.TrainingListener;
// import ai.djl.training.loss.Loss;
// import ai.djl.training.optimizer.Optimizer;
// import ai.djl.training.tracker.Tracker;
import ai.djl.training.util.ProgressBar;
// import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import jakarta.annotation.PostConstruct;

@Service
public class ModelService {
    Logger logger = LoggerFactory.getLogger(ModelService.class);

    private static final String SAVED_MODEL_ARCHIVE_EXTENSION = ".zip";
    private static final String DEFAULT_MODEL_KEY = "14_57_38_30_10_2023";
    public static final String DEFAULT_MODEL_NAME = "saved_model-" + DEFAULT_MODEL_KEY + SAVED_MODEL_ARCHIVE_EXTENSION;

    private List<InMemoryModel> inMemoryModels = new ArrayList<>();

    private final ModelRepository modelRepository;
    private final ImageRepository imageRepository;

    public ModelService(ModelRepository repository, ImageRepository imageRepository) {
        this.modelRepository = repository;
        this.imageRepository = imageRepository;
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
            Files.createDirectories(Paths.get(com.jtrull.alzdetection.general.Utils.returnModelPath()));
        } catch (Exception ex) {
            throw new FailedRequirementException(ex, HttpStatus.CONFLICT, 
                "Could not create the directory where uploaded model files will be stored.");
        }

        // load default model into DB
        loadDefaultModel();

        // init the in-memory models
        initInMemoryModels();
    }

    /**
     * Given a .zip MultipartFile as input, move this file to the
     * /resources/model/{hash(file.getName())} and load the Model into the database.
     * 
     * @param file
     * @return Model - created from this input MultipartFile
     */
    public Model loadModelFromFile(MultipartFile model) {
        logger.trace("entered loadModelFromFile for model '" + model + "'");
        Path destinationFile;

        // Error checks
        if (model.isEmpty()) {
            throw new InvalidModelFileException(model, "Found empty file");
        }
        if (!FilenameUtils.getExtension(model.getOriginalFilename()).equals("zip")) {
            throw new InvalidModelFileException(model);
        }

        int hashDir = Math.abs(model.getOriginalFilename().hashCode());
        // Create directory to hold new model, constructed of a hash of the model's name.
        // This assumption implies unique model zip names
        Path newPath = Paths.get(Utils.returnModelPath() + "/" + hashDir);
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new FailedRequirementException(e, "Unable to create new directory to store model at: " + newPath);
        }

        // Copy the model .zip to the resources directory
        destinationFile = newPath.resolve(Paths.get(model.getOriginalFilename())).normalize().toAbsolutePath();
        try (InputStream inputStream = model.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FailedRequirementException(e,  "Unable to move model zip file '" + model.getName() + "' to destination: " + destinationFile);
        }

        // Find the model.zip in its subdirectory in the resources directory,
        // create a Model object and save to the database
        File resource = findModelResourceInDir(destinationFile.toFile().getName());
        Model modelObj = createModelFromFilepath(resource);

        synchronized (modelRepository) {
            modelRepository.save(modelObj);
        }

        Optional<Model> found = modelRepository.findAll().stream()
                .filter(m -> m.getFilepath().equals(resource.getParent()))
                .filter(m -> m.getName().equals(resource.getName()))
                .findAny();

        if (found.isEmpty()) {
            synchronized (modelRepository) {
                modelRepository.delete(modelObj);
            }
            throw new FailedRequirementException(new NoSuchElementException(resource.toString()),  
                "Unable to find model after create! Unable to validate model was created successfully. Deleting model.");
        }

        // add model into memory with ID populated from repository
        return addModelToInMemoryModels(found.get());
    }

    public Model updateModelInRepository(Model updatedModel) {
        synchronized (modelRepository) {
            modelRepository.save(updatedModel);
        }
        return updatedModel;
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
     * Given a model's filename, attempt to find the file in the expected directory
     * where models are stored.
     * 
     * @param filename
     * @return File - corresponding to this model if found
     */
    public File findModelResourceInDir(String filename) {
        Optional<File> resource = getSavedModelInResourcesDir(filename);
        if (resource.isEmpty()) {
            throw new ModelNotFoundException(filename);
        };
        return resource.get();
    }

    /**
     * Get the model at the specified ID in the modelRepository. If unable to be found,
     * throw ModelNotFoundException.
     * 
     * @param modelId
     * @return
     */
    public Model getModelById(Long modelId) {
        return modelRepository.findById(modelId).stream()
            .findAny()
            .orElseThrow(() -> 
                new ModelNotFoundException(modelId));
    }

    /**
     * Iterate through all in-memory models, find the model with the specified ID and return.
     * If unable to be found, throw ModelNotFoundException.
     * 
     * @param modelId
     * @return
     */
    public InMemoryModel getInMemoryModelById(Long modelId) {
        return getInMemoryModels().stream()
            .filter(m -> m.getId() == modelId)
            .findAny()
            .orElseThrow(() -> 
                new ModelNotFoundException(modelId));
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
            throw new UnsupportedOperationException("Unable to delete default model");
        }
        if (modelRepository.findById(modelId).isEmpty()) {
            throw new ModelNotFoundException(modelId);
        }

        try {
            synchronized (inMemoryModels) {
                Optional<InMemoryModel> opt = inMemoryModels.stream().filter(m -> m.getId() == modelId).findFirst();
                if (opt.isPresent()) {
                    opt.get().close();
                    inMemoryModels.remove(opt.get());
                }
            }
            synchronized (modelRepository) {
                modelRepository.deleteById(modelId);
            }

            // Delete all images in image repository associated with this model
            synchronized (imageRepository) {
                List<ImagePrediction> imagesToDelete = imageRepository.findAll().stream()
                        .filter(i -> i.getAssociatedModel() == modelId)
                        .collect(Collectors.toList());
                imageRepository.deleteAll(imagesToDelete);
            }

        } catch (Exception e) {
            throw new UnexpectedDeleteErrException(e, "Error occurred during deletion of model with Id: '" + modelId + "'");
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
                List<InMemoryModel> removable = inMemoryModels.stream()
                    .filter(m -> m.getId() != 1)
                    .map(m->m.close())
                    .collect(Collectors.toList());
                inMemoryModels.removeAll(removable);
                
            }

            synchronized (modelRepository) {
                List<Model> removable = modelRepository.findAll().stream()
                        .filter(m -> m.getId() != 1)
                        .collect(Collectors.toList());
                modelRepository.deleteAllInBatch(removable);
            }
        } catch (Exception e) {
            throw new UnexpectedDeleteErrException(e, "Error occurred during deletion of all non-default models");
        }
        return true;
    }

    /**
     * Load the Model object into a Translator and Criteria to be used with the
     * Amazon Tensorflow Java API DJL.
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
     * Given the specified path, attempt to find the file with the given filename
     * and extension.
     * 
     * @param filename
     * @param path
     * @return
     */
    private Optional<File> returnFileFromPath(String filename, String path) {
        try {
            return Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(r -> r.toFile())
                    .filter(r -> r.getName().equals(filename))
                    .findFirst();

        } catch (IOException e) {
            throw new FailedRequirementException(e, "Error while traversing path '" + path + "' to find model with name '" + filename + "'");
        }
    }

    /**
     * Find a model with the specified filename in the resources/model/ directory.
     * 
     * @param filename
     * @return
     */
    public Optional<File> getSavedModelInResourcesDir(String filename) {
        String path = Utils.returnModelPath();
        try {
            Optional<File> defaultModelInBasePath = returnFileFromPath(filename, path);
            if (defaultModelInBasePath.isPresent()) {
                return defaultModelInBasePath;
            }
            // find all files
            return Files.walk(Paths.get(path))
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .map(x -> returnFileFromPath(filename, x.getAbsolutePath()))
                    .findFirst().get();

        } catch (IOException e) {
            throw new FailedRequirementException(e, 
                "Error while traversing path '" + path + "' to find model in resources directory with name '" + filename + "'");
        }
    }

    /**
     * Add a Model object to the map of current in-memory Criteria objects. The
     * Amazon Tensorflow API can take a bit of time to load in models,
     * and these models once loaded into memory are not Serializable. Store all the
     * required information to spin up a model in the database,
     * the Model object, then load the models in the database into memory by
     * creating Criteria objects.
     * 
     * @param m
     * @return
     */
    public Model addModelToInMemoryModels(Model m) {
        Criteria<Image, Classifications> criteria = loadModelIntoTensorflow(m);
        InMemoryModel inMemModel = new InMemoryModel(this, m.getId(), criteria);
        synchronized (inMemoryModels) {
            inMemoryModels.add(inMemModel);
        }

        if (modelRepository.findById(m.getId()).isPresent() && !modelRepository.findById(m.getId()).get().equals(m)) {
            synchronized (modelRepository) {
                modelRepository.save(m);
            }
        }
        
        // trainModelOnTestData(inMemModel);

        return m;
    }



    /**
     * Initialize the in-memory models by finding all Models in the ModelRepository
     * and adding in-memory representations of them.
     */
    public void initInMemoryModels() {
        modelRepository.findAll().stream().forEach(m -> {
            m = m.findPlotForKey();
            m = m.findPropertiesForKey();
            addModelToInMemoryModels(m);
        });
    }

    /**
     * Get all current in-memory models.
     * 
     * @return
     */
    public List<InMemoryModel> getInMemoryModels() {
        return this.inMemoryModels;
    }

    ImagePrediction convertClassificationsToPrediction(Classifications classifications, File file, ImpairmentEnum actualImpairmentValue, Long modelId) {
        // Gather a map of confidences used to populate the ImagePrediction returned from this method
        HashMap<ImpairmentEnum, Integer> confidences = new HashMap<>();
        for (Classification item : classifications.items()) {
            ImpairmentEnum enumVal = ImpairmentEnum.fromString(item.getClassName());
            confidences.put(enumVal, (int) (item.getProbability() * 100));
        }

        // Return new ImagePrediction object
        return new ImagePrediction(
            file.toPath().toString(), 
            confidences.get(ImpairmentEnum.NO_IMPAIRMENT),
            confidences.get(ImpairmentEnum.VERY_MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MODERATE_IMPAIRMENT),
            actualImpairmentValue, 
            modelId);
    }
}
