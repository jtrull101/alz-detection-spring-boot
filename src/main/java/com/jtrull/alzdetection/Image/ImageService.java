package com.jtrull.alzdetection.Image;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;

import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import jakarta.annotation.PostConstruct;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

@Service
public class ImageService {
    Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final Path root = Paths.get("images");

    private final ImageRepository imageRepository;
    private final ModelRepository modelRepository;
    private final ModelService modelService;

    HashMap<ImpairmentEnum, List<File>> testFiles = new HashMap<>();

    public ImageService(ImageRepository imageRepository, ModelRepository modelRepository, ModelService modelService){
        this.imageRepository = imageRepository;
        this.modelRepository = modelRepository;
        this.modelService = modelService;
    }

    private static final String DATASET_NAME = "Combined Dataset";
    private static final String ARCHIVE_FORMAT = ".zip";
    private static final String IMAGE_TYPE = ".jpg";

    @PostConstruct
    public void init() {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        logger.info("found zipped combined dataset directory: " + foundZipFiles);
        for (File f : foundZipFiles) {
            ZipFile zipfile = new ZipFile(f);
            try {
                logger.info("unzipping file " + zipfile + " to destination: " + resource.toString() + "/" + DATASET_NAME);
                zipfile.extractAll(resource.toString() + "/" + DATASET_NAME);
            } catch (ZipException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                // throw new Exception("Unable to unzip Dataset directory: " + foundZipFiles);
            } finally {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        Optional<File> opt = Optional.empty();
        try {
            opt = Files.walk(Paths.get(resource.toURI()))
                    .filter(Files::isDirectory)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME))
                    .map(x -> x.toFile())
                    .findFirst();        
        } catch (IOException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (opt.isEmpty()) {
            logger.error("Unable to find Combined Dataset directory after unzipping");
            assert false;
        }
        
        File foundCombinedDatasetDir = opt.get();
        logger.info("found unzipped combined dataset directory: " + foundCombinedDatasetDir);


        // populate testFiles hashmap of all test images mapped to their corresponding categories
        List<File> impairmentCategories = null;
        URI resourceURI = null;
        try {
            resourceURI = resource.toURI();
            impairmentCategories = Files.walk(Paths.get(resourceURI + "/test"))
                .filter(Files::isDirectory)
                .map(x -> x.toFile())
                .collect(Collectors.toList());

        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        }

        if (impairmentCategories == null) {
            logger.error("Unable to find impairment category subdirectories in dataset at location: " + resourceURI + "/test");
            assert false;
            return;
        }

        for (File f : impairmentCategories) {
            ImpairmentEnum category = ImpairmentEnum.valueOf(f.getName());

            List<File> images = new ArrayList<>();

            try {
                 images = Files.walk(f.toPath())
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(IMAGE_TYPE))
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            testFiles.put(category, images);
        }
    }


    /**
     * 
     * @param file
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForImage(MultipartFile file, Long modelNum) throws Exception {
        
        // check repository if prediction for this image has already been fetched
        
        // fetch model from model repo
        if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        }
        SavedModelBundle bundle = modelService.getInMemoryModels().get(modelNum);


        ImagePrediction prediction = runPredictionOnModel(bundle, null);
        imageRepository.save(prediction);
        return prediction;
    }

    /**
     *  
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomImage(Long modelNum) throws Exception {
        // fetch model from model repo
        
        if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        }
        SavedModelBundle bundle = modelService.getInMemoryModels().get(modelNum);
        
       
        // find random sample in test set

        // if not in repository, run prediction and add to repository
        ImagePrediction prediction = runPredictionOnModel(bundle, null);
        imageRepository.save(prediction);
        return prediction;
    }

    /**
     * 
     * @param impairment
     * @return
     * @throws Exception
     */
    public ImagePrediction runPredictionForRandomFromImpairmentCategory(String impairment, Long modelNum) throws Exception {
        // download test set if not present

        // find random sample in test set for specific impairment category

        // if not in repository, run prediction and add to repository
         if (!modelService.getInMemoryModels().containsKey(modelNum)) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        }
        SavedModelBundle bundle = modelService.getInMemoryModels().get(modelNum);
        ImagePrediction prediction = runPredictionOnModel(bundle, null);
        imageRepository.save(prediction);
        return prediction;
    }


    private ImagePrediction runPredictionOnModel(SavedModelBundle bundle, File toPredict) {
        
        return null;
    }
    
}
