package com.jtrull.alzdetection.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.springframework.stereotype.Service;
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

    HashMap<ImpairmentEnum, List<File>> testFiles = new HashMap<>();

    public ImageService(ImageRepository imageRepository, ModelService modelService) {
        this.imageRepository = imageRepository;
        this.modelService = modelService;
    }

    private static final String DATASET_NAME = "Combined Dataset";
    private static final String ARCHIVE_FORMAT = ".zip";
    private static final String IMAGE_TYPE = ".jpg";

    @PostConstruct
    public void init() {
        initializeTestImages();
    }

    /**
     * 
     * @param file
     * @return
     * @throws Exception
     */
    
    public ImagePrediction runPredictionForImage(MultipartFile file, Long modelNum) {
        logger.info("entered runPredictionForImage for file: " + file);
        Path destinationFile;
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            
            String filename = (file.getOriginalFilename() == null) ? file.getName() + file.getBytes().hashCode() : file.getOriginalFilename();
            @SuppressWarnings("all")
            Path newPath = Paths.get(root + "/"  + filename.hashCode());
            
            Files.createDirectories(newPath);
            destinationFile = newPath.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(destinationFile.toFile(), modelNum);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // fetch model from model repo
        if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new RuntimeException("Unable to run a prediction with model of number " + modelNum
                    + " that is missing from the database");
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelNum);

        ImagePrediction prediction = runPredictionOnModel(modelNum, criteria, destinationFile.toFile(), null);
        imageRepository.save(prediction);
        return prediction;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomImage(Long modelNum) {
        // fetch model from model repo
        if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new RuntimeException("Unable to run a prediction with model of number " + modelNum
                    + " that is missing from the database");
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelNum);

        // find random sample in test set
        Random generator = new Random();
        Object[] vals = testFiles.values().toArray();
        int categoryLabelIndex = generator.nextInt(vals.length);
        ImpairmentEnum categoryLabel = (ImpairmentEnum) testFiles.keySet().toArray()[categoryLabelIndex];
        logger.info("chosen random category for random prediction: " + categoryLabel.toString());

        List<File> images = testFiles.get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelNum);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // If image repository had no previously predicted data, run a prediction and save into the repository
        ImagePrediction prediction = runPredictionOnModel(modelNum, criteria, randomImage, categoryLabel);
        imageRepository.save(prediction);
        logger.debug("Added new image prediction: " + prediction + " to image database");
        return prediction;
    }

    /**
     * 
     * @param file
     * @param modelNum
     * @return
     */
    public Optional<ImagePrediction> findImagePredictionInRepoByFileAndModel(File file, Long modelNum) {
        Optional<ImagePrediction> image = imageRepository.findAll().stream()
            .filter(p -> p.getFilepath().equals(file.getAbsolutePath()))
            .filter(p -> p.getAssociatedModel().equals(modelNum))
            .findAny();
        logger.debug("Found existing prediction for file: " + image + " in database, returning");
        return image;
    }


    public boolean runDeletePrediction(long fileId, long modelId) {
        Optional<ImagePrediction> image = imageRepository.findById(fileId);
        if (image.isEmpty()) { 
            throw new IllegalStateException("Unable to delete prediction for image with id: " + fileId);
        }
        imageRepository.delete(image.get());
        return true;
    }

    /**
     * 
     * @param impairment
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomFromImpairmentCategory(String impairment, Long modelNum) {
        
        // find random sample in test set for specific impairment category
        Optional<ImpairmentEnum> opt = ImpairmentEnum.fromString(impairment);
        if (opt.isEmpty()) {
            throw new RuntimeException("Unable to parse category: " + impairment + ". Expected values=[" + Arrays.asList(ImpairmentEnum.asStrings().toArray()) + "]");
        }
        ImpairmentEnum categoryLabel = opt.get();
        Random generator = new Random();
        List<File> images = testFiles.get(categoryLabel);
        File randomImage = images.get(generator.nextInt(images.size()));

        // Check image repository for previous predictions with this image and model number
        Optional<ImagePrediction> existingPrediction = findImagePredictionInRepoByFileAndModel(randomImage, modelNum);
        if (existingPrediction.isPresent()) return existingPrediction.get();

        // if not in repository, run prediction and add to repository
        if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new RuntimeException("Unable to run a prediction with model of number " + modelNum
                    + " that is missing from the database");
        }
        Criteria<Image, Classifications> criteria = modelService.getInMemoryModels().get(modelNum);
        ImagePrediction prediction = runPredictionOnModel(modelNum, criteria, randomImage, categoryLabel);
        imageRepository.save(prediction);
        return prediction;
    }

    /**
     * Using DJL (deep java library found on GitHub) leverage tensorflow for
     * predictions
     * 
     * @param bundle
     * @param toPredict
     * @return
     */
    private ImagePrediction runPredictionOnModel(Long modelNum, Criteria<Image, Classifications> criteria, File toPredict, ImpairmentEnum actualImpairmentValue) {
        Image image;
        try {
            image = ImageFactory.getInstance().fromFile(toPredict.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error loading image into image factory: " + e.getMessage());
        }

        Classifications result;
        try (ZooModel<Image, Classifications> zooModel = ModelZoo.loadModel(criteria)) {
            try (Predictor<Image,Classifications> predictor = zooModel.newPredictor()) {
                try {
                    result = predictor.predict(image);
                } catch (TranslateException e) {
                    throw new RuntimeException(
                            "Error while running prediction for image: " + image + ". Message = " + e.getMessage());
                }
                logger.info("Diagnose: {}", result);
            }

        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException("Error while loading model: " + criteria + " Message = " + e.getMessage());
        }
        
        HashMap<ImpairmentEnum, Integer> confidences = new HashMap<>();
        for (Classification item : result.items()) {
            int percent = (int) (item.getProbability() * 100);
            Optional<ImpairmentEnum> enumVal = ImpairmentEnum.fromString(item.getClassName());
            confidences.put(enumVal.get(), percent);
        }

        return new ImagePrediction(
            toPredict.toPath().toString(), 
            confidences.get(ImpairmentEnum.NO_IMPAIRMENT),
            confidences.get(ImpairmentEnum.VERY_MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MILD_IMPAIRMENT),
            confidences.get(ImpairmentEnum.MODERATE_IMPAIRMENT),
            actualImpairmentValue, 
            modelNum);
    } 


    /**
     * 
     */
    public void initializeTestImages() {
        try {
            Path path = Files.createDirectories(this.root);
            logger.info("created path: " + path);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }

        // setup test set
        ClassLoader cl = getClass().getClassLoader();
        URL resource = cl.getResource(root.toString());
        logger.info("searching for " + DATASET_NAME + ARCHIVE_FORMAT + " in directory: " + root);
        List<File> foundZipFiles = new ArrayList<>();
        try {
            foundZipFiles = Files.walk(Paths.get(resource.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME + ARCHIVE_FORMAT))
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error while finding archive of dataset images", e);
        }

        logger.info("found zipped combined dataset directory: " + foundZipFiles);
        for (File f : foundZipFiles) {
            Path targetPath = Paths.get(resource.getPath());
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(f.toPath()))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    final Path toPath = targetPath.resolve(entry.getName());
                    if (toPath.toString().contains("/train")) {
                        // ignore training data
                        continue;
                    }
                    // create directories for nested zip
                    if (entry.isDirectory()) {
                        if (!toPath.toFile().exists()) {
                            logger.info("creating required subdirectory: " + toPath);
                            Files.createDirectory(toPath);
                        }
                    } else {
                        if (!toPath.toFile().exists()) {
                            logger.info("unzipping file: " + zipInputStream + " to path: " + toPath);
                            Files.copy(zipInputStream, toPath);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while uncompressing dataset archive", e);
            }
        }

        Optional<File> opt = Optional.empty();
        URI resourceURI = null;
        try {
            resourceURI = resource.toURI();
            opt = Files.walk(Paths.get(resourceURI))
                    .filter(Files::isDirectory)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME))
                    .map(x -> x.toFile())
                    .findFirst();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error while finding unzipped dataset", e);
        }

        if (opt.isEmpty()) {
            logger.error("Unable to find Combined Dataset directory after unzipping. Check if " + DATASET_NAME
                    + " exists at location: " + resourceURI);
            assert false;
        }

        File foundCombinedDatasetDir = opt.get();
        logger.info("found unzipped combined dataset directory: " + foundCombinedDatasetDir);

        // populate testFiles hashmap of all test images mapped to their corresponding
        // categories
        List<File> impairmentCategories = null;
        Path testDirPath = Paths.get(resourceURI.getPath() + "/" + DATASET_NAME + "/test/");
        try {
            impairmentCategories = Files.walk(testDirPath)
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Error while finding /test directory at location: " + testDirPath);
        }

        if (impairmentCategories == null) {
            logger.error("Unable to find impairment category subdirectories in dataset at location: " + testDirPath);
            assert false;
            return;
        }

        for (File f : impairmentCategories) {
            String name = f.getName().replaceAll("\\P{Print}", "");
            if (name.equals("test"))
                continue; // exclude parent directory
            logger.info("processing potential impairment category: " + name);
            Optional<ImpairmentEnum> category = ImpairmentEnum.fromString(name);
            if (category.isEmpty()) {
                throw new RuntimeException(
                        "Error while determining ImpairmentEnum category from directory name: " + name);
            }

            List<File> images = new ArrayList<>();
            try {
                images = Files.walk(f.toPath())
                        .filter(Files::isRegularFile)
                        .filter(r -> r.getFileName().toString().contains(IMAGE_TYPE))
                        .map(x -> x.toFile())
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Error while gathering images from category " + name);
            }

            if ((testFiles.containsKey(category.get()) && testFiles.get(category.get()).size() < images.size())
                    || !testFiles.containsKey(category.get())) {
                logger.info("adding " + images.size() + " images to category: " + category.get());
                testFiles.put(category.get(), images);
            }
        }
    }
}
