package com.jtrull.alzdetection.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jtrull.alzdetection.Utils;
import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;
import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;
import com.jtrull.alzdetection.exceptions.predictions.PredictionFailureException;
import com.jtrull.alzdetection.exceptions.predictions.PredictionNotFoundException;

import jakarta.annotation.PostConstruct;


@Service
public class ImageService {
    Logger logger = LoggerFactory.getLogger(ImageService.class);


    private final ImageRepository imageRepository;
    private final ModelService modelService;
    private final Utils utils;

    public ImageService(ImageRepository imageRepository, ModelService modelService, Utils utils) {
        this.imageRepository = imageRepository;
        this.modelService = modelService;
        this.utils = utils;
    }
    
    @PostConstruct
    public void init() {
        this.utils.initializeTestImages();
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
                throw new PredictionFailureException(file, "Failed to store empty file");
            }
            
            // TOOD: Uncomment if interested in not running the prediction if filenames are the same
            //      String filename = (file.getOriginalFilename() == null) ? file.getName() + file.getBytes().hashCode() : file.getOriginalFilename();
            String filename = file.getName() + file.getBytes().hashCode();

            Path newPath = Paths.get(this.utils.returnImagePath() + "/"  + modelId + "/" + filename.hashCode());
            Files.createDirectories(newPath);
            destinationFile = newPath.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new FailedRequirementException(e, "Failed to copy new image: '" + file + "'' to required directory '" + destinationFile + "'");
        }

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(destinationFile.toFile(), modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // fetch model from model repo
        ImagePrediction prediction = modelService.getInMemoryModelById(modelId).predictOnModel(destinationFile.toFile(), null);

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
        // find random sample in test set
        Random generator = new Random();
        Object[] vals = this.utils.getTestFiles().values().toArray();
        int categoryLabelIndex = generator.nextInt(vals.length);
        ImpairmentEnum categoryLabel = (ImpairmentEnum) this.utils.getTestFiles().keySet().toArray()[categoryLabelIndex];
        logger.info("chosen random category for random prediction: " + categoryLabel.toString());

        List<File> images = this.utils.getTestFiles().get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // If image repository had no previously predicted data, run a prediction and save into the repository
        ImagePrediction prediction = modelService.getInMemoryModelById(modelId).predictOnModel(randomImage, categoryLabel);

        synchronized (imageRepository) {
            imageRepository.save(prediction);
        }
        return prediction;
    }

    /**
     * Check the Image database for a prediction that has already occurred for the specified File and the modelId
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
        ImagePrediction image = imageRepository.findById(predictionId).orElseThrow(() -> 
            new PredictionNotFoundException(predictionId));
        if (image.getAssociatedModel() != modelId) {
            throw new PredictionNotFoundException(predictionId, "Error with request, unable to find prediction for modelId '" + modelId + "'");
        }
        return image;
    }

    /**
     * Delete a prediction present in the Image database. Useful if a user desired to remove their potentially sensitive data from the server.
     * 
     * @param fileId
     * @param modelId
     * @return
     */
    public boolean runDeletePrediction(long fileId, long modelId) {
        ImagePrediction image = imageRepository.findById(fileId).orElseThrow(() -> 
            new PredictionNotFoundException(fileId, "Unable to find prediction with prediction ID and model ID", 
                String.valueOf(fileId), String.valueOf(modelId)));

        synchronized (imageRepository) {
            imageRepository.delete(image);
        }

        return true;
    }

    /**
     * TODO
     * 
     * @param modelId
     * @return
     */
    public List<ImagePrediction> runPredictionForEveryTestFile(long modelId) {
        List<File> flattenedFiles = this.utils.getTestFiles().values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        return modelService.getInMemoryModelById(modelId)
            .batchPredict(flattenedFiles, null);
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
        ImpairmentEnum categoryLabel = ImpairmentEnum.fromString(impairment);
        Random generator = new Random();
        List<File> images = this.utils.getTestFiles().get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelId);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // if not in repository, run prediction and add to repository
        ImagePrediction prediction = modelService.getInMemoryModelById(modelId).predictOnModel(randomImage, categoryLabel);

        synchronized (imageRepository) {
            imageRepository.save(prediction);
        }
        return prediction;
    }

   
}
