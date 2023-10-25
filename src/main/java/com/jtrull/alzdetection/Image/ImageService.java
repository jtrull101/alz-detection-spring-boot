package com.jtrull.alzdetection.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;


@Service
public class ImageService {
    Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final Path root = Paths.get("images");

    private final ImageRepository imageRepository;
    private final ModelService modelService;
    
    private static final String DATASET_NAME = "Combined Dataset";
    private static final String ARCHIVE_FORMAT = ".zip";
    private static final String IMAGE_TYPE = ".jpg";

    HashMap<ImpairmentEnum, List<File>> testFiles = new HashMap<>();

    public ImageService(ImageRepository imageRepository, ModelService modelService) {
        this.imageRepository = imageRepository;
        this.modelService = modelService;
    }
    
    @PostConstruct
    public void init() {
        initializeTestImages();
    }

    /**
     * Given an image passed into the /predict endpoint, run a prediction given the specified Tensorflow model Id. 
     *  The resulting ImagePrediction will give confidences for each Impairment category, providing as accurate
     *  of a diagnosis as the model is able to perform.
     * 
     * @param file
     * @param modelId
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForImage(MultipartFile file, Long modelId) {
        logger.trace("entered runPredictionForImage for file: " + file);
        Path destinationFile = null;
        try {
            if (file.isEmpty()) {
                throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Failed to store empty file.");
            }
            
            // TOOD: Uncomment if interested in not running the prediction if filenames are the same
            //      String filename = (file.getOriginalFilename() == null) ? file.getName() + file.getBytes().hashCode() : file.getOriginalFilename();
            String filename = file.getName() + file.getBytes().hashCode();

            Path newPath = Paths.get(returnImagePath() + "/"  + modelId + "/" + filename.hashCode());
            Files.createDirectories(newPath);
            destinationFile = newPath.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(406), 
                    "Failed to copy new image: " + file + " to required directory " + destinationFile + " message  = " + e.getMessage());
        }

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(destinationFile.toFile(), modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // fetch model from model repo
        if (!modelService.getInMemoryModels().containsKey(modelId)) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model with Id: " + modelId);
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelId);

        ImagePrediction prediction = runPredictionOnModel(modelId, criteria, destinationFile.toFile(), null);
        synchronized (imageRepository) {
            imageRepository.save(prediction);
        }
        return prediction;
    }

    /**
     * Given the specified Tensorflow modelId, grab a random image from all possible Impairment categories and return the prediction.
     * 
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomImage(Long modelId) {
        // fetch model from model repo
        if (!modelService.getInMemoryModels().containsKey(modelId)) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model with Id: " + modelId);         
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelId);

        // find random sample in test set
        Random generator = new Random();
        Object[] vals = testFiles.values().toArray();
        int categoryLabelIndex = generator.nextInt(vals.length);
        ImpairmentEnum categoryLabel = (ImpairmentEnum) testFiles.keySet().toArray()[categoryLabelIndex];
        logger.info("chosen random category for random prediction: " + categoryLabel.toString());

        List<File> images = testFiles.get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // If image repository had no previously predicted data, run a prediction and save into the repository
        ImagePrediction prediction = runPredictionOnModel(modelId, criteria, randomImage, categoryLabel);
        synchronized (imageRepository) {
            imageRepository.save(prediction);
        }
        logger.debug("Added new image prediction: " + prediction + " to image database");
        return prediction;
    }

    /**
     * Check the Image database for a prediction that has already occured for the specified File and the modelId
     * 
     * @param file
     * @param modelId
     * @return
     */
    public Optional<ImagePrediction> findImagePredictionInRepoByFileAndModel(File file, Long modelId) {
        Optional<ImagePrediction> image = imageRepository.findAll().stream()
            .filter(p -> p.getFilepath().equals(file.getAbsolutePath()))
            .filter(p -> p.getAssociatedModel().equals(modelId))
            .findAny();
        logger.debug("Found existing prediction for file: " + image + " in database, returning");
        return image;
    }

     public ImagePrediction runGetPrediction(long predictionId, Long modelId) {
        Optional<ImagePrediction> image = imageRepository.findById(predictionId);
        if (image.isEmpty()) { 
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find prediction with Id: " + predictionId);
        }

        if (image.get().getAssociatedModel() != modelId) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Error with request, unable to find prediction for modelId " + modelId);
        }

        return image.get();
    }

    /**
     * Delete a prediction present in the Image database. Useful if a user desired to remove their potentially sensitive data from the server.
     * 
     * @param fileId
     * @param modelId
     * @return
     */
    public boolean runDeletePrediction(long fileId, long modelId) {
        Optional<ImagePrediction> image = imageRepository.findById(fileId);
        if (image.isEmpty()) { 
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find prediction with Id: " + fileId);
        }
        synchronized (imageRepository) {
            imageRepository.delete(image.get());
        }
        return true;
    }

    /**
     * Given a specified Impairment category, run a prediction on the specified Tensorflow model. Return this prediction
     *  and the confidences. 
     * 
     * @param impairment
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomFromImpairmentCategory(String impairment, Long modelId) {
        
        // find random sample in test set for specific impairment category
        Optional<ImpairmentEnum> opt = ImpairmentEnum.fromString(impairment);
        if (opt.isEmpty()) { 
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), 
                 "Unable to parse category: " + impairment + ". Expected values=[" + Arrays.asList(ImpairmentEnum.asStrings().toArray()) + "]");
        }
        ImpairmentEnum categoryLabel = opt.get();
        Random generator = new Random();
        List<File> images = testFiles.get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // if not in repository, run prediction and add to repository
        if (!modelService.getInMemoryModels().containsKey(modelId)) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model with Id: " + modelId);
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelId);
        ImagePrediction prediction = runPredictionOnModel(modelId, criteria, randomImage, categoryLabel);
        synchronized (imageRepository) {
            imageRepository.save(prediction);
        }
        return prediction;
    }

    /**
     * Using DJL (deep java library found on GitHub) leverage tensorflow for predictions
     * 
     * @param bundle
     * @param toPredict
     * @return
     */
    private ImagePrediction runPredictionOnModel(Long modelId, Criteria<Image, Classifications> criteria, File toPredict, ImpairmentEnum actualImpairmentValue) {
        Image image;
        try {
            image = ImageFactory.getInstance().fromFile(toPredict.toPath());
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Error while loading image " + toPredict.toPath() + " is it a valid image?");
        }

        // Load model into the 'ModelZoo' environment used by the Amazon DJL framework to run a prediction
        Classifications result;
        try (ZooModel<Image, Classifications> zooModel = ModelZoo.loadModel(criteria)) {
            try (Predictor<Image,Classifications> predictor = zooModel.newPredictor()) {
                try {
                    result = predictor.predict(image);
                    logger.info("Diagnose: {}", result);
                } catch (TranslateException e) {
                    throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Error while running prediction for image: " + image + ". Message = " + e.getMessage());
                }
            }

        } catch (IOException | MalformedModelException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Error while loading model: " + criteria + " Message = " + e.getMessage());
        } catch (ModelNotFoundException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find model for criteria: " + criteria + " Message = " + e.getMessage());
        }
        
        // Gather a map of confidences used to populate the ImagePrediction returned from this method
        HashMap<ImpairmentEnum, Integer> confidences = new HashMap<>();
        for (Classification item : result.items()) {
            Optional<ImpairmentEnum> enumVal = ImpairmentEnum.fromString(item.getClassName());
            confidences.put(enumVal.get(), (int) (item.getProbability() * 100));
        }

        // Return new ImagePrediction object
        return new ImagePrediction(
            toPredict.toPath().toString(), 
            confidences.get(ImpairmentEnum.NO_IMPAIRMENT),
            confidences.get(ImpairmentEnum.VERY_MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MODERATE_IMPAIRMENT),
            actualImpairmentValue, 
            modelId);
    } 

    /**
     * Return the path where models will be stored, represented in some way by /resource/model
     * 
     * @return
     */
    public String returnImagePath() {
        return getClass().getResource("/").getPath() + root;
    }

    /**
     * Given the Dataset .zip that is supplied with this application, populate the map of test images per impairment category. 
     *  These images are used in the /random prediction endpoint to supply some dummy MRI data
     */
    public void initializeTestImages() {

        Path p;
        try {
            p = Files.createDirectories(Paths.get(returnImagePath()));
        } catch (Exception ex) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(409), "Could not create the directory where uploaded model files will be stored.");
        }

        // Find the Zipped dataset and unzip if found
        logger.trace("searching for " + DATASET_NAME + ARCHIVE_FORMAT + " in directory: " + p);
        List<File> foundZipFiles = new ArrayList<>();
        try {
            foundZipFiles = Files.walk(p)
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME + ARCHIVE_FORMAT))
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find dataset with name '" + DATASET_NAME + ARCHIVE_FORMAT + "'. message = " + e.getMessage());
        }

        logger.trace("found zipped combined dataset directory: " + foundZipFiles);
        for (File f : foundZipFiles) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(f.toPath()))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    final Path toPath = p.resolve(entry.getName());
                    if (toPath.toString().contains("/train")) {
                        // ignore training data
                        continue;
                    }
                    // create directories for nested zip
                    if (entry.isDirectory()) {
                        if (!toPath.toFile().exists()) {
                            logger.trace("creating required subdirectory: " + toPath);
                            Files.createDirectory(toPath);
                        }
                    } else {
                        if (!toPath.toFile().exists()) {
                            Files.copy(zipInputStream, toPath);
                        }
                    }
                }
            } catch (IOException e) {
                throw new HttpClientErrorException (HttpStatusCode.valueOf(406), "Error while uncompressing dataset archive. message = " + e.getMessage());
            }
        }

        // Now the contents have been unzipped. Search for the uncompressed dataset
        Optional<File> opt = Optional.empty();
        try {
            opt = Files.walk(p)
                    .filter(Files::isDirectory)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME))
                    .map(x -> x.toFile())
                    .findFirst();
        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find unzipped dataset with name '" + DATASET_NAME + "'. message = " + e.getMessage());
        }
        if (opt.isEmpty()) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find Combined Dataset directory after unzipping. Check if " + DATASET_NAME + " exists at location: " + p);
        }

        File foundCombinedDatasetDir = opt.get();
        logger.trace("found unzipped combined dataset directory: " + foundCombinedDatasetDir);

        // populate testFiles hashmap of all test images mapped to their corresponding categories
        List<File> impairmentCategories = null;
        Path testDirPath = Paths.get(p + "/" + DATASET_NAME + "/test/");
        try {
            impairmentCategories = Files.walk(testDirPath)
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Error while finding /test directory at location: " + testDirPath);
        }
        if (impairmentCategories == null) {
            throw new HttpClientErrorException (HttpStatusCode.valueOf(404), "Unable to find impairment category subdirectories in dataset at location: " + testDirPath);
        }

        for (File f : impairmentCategories) {
            String name = f.getName().replaceAll("\\P{Print}", "");
            if (name.equals("test")) continue; // exclude parent directory
            
            logger.trace("processing potential impairment category: " + name);
            Optional<ImpairmentEnum> category = ImpairmentEnum.fromString(name);
            if (category.isEmpty()) {
                throw new HttpClientErrorException (HttpStatusCode.valueOf(406), "Error while determining ImpairmentEnum category from directory name: " + name);
            }

            List<File> images = new ArrayList<>();
            try {
                images = Files.walk(f.toPath())
                        .filter(Files::isRegularFile)
                        .filter(r -> r.getFileName().toString().contains(IMAGE_TYPE))
                        .map(x -> x.toFile())
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new HttpClientErrorException (HttpStatusCode.valueOf(400), "Error while gathering images from category " + name);
            }

            if ((testFiles.containsKey(category.get()) && testFiles.get(category.get()).size() < images.size()) || !testFiles.containsKey(category.get())) {
                logger.info("adding " + images.size() + " images to category: " + category.get());
                testFiles.put(category.get(), images);
            }
        }
    }

   
}
