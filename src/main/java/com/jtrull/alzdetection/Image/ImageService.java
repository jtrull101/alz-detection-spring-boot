package com.jtrull.alzdetection.Image;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;

import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;

@Service
public class ImageService {
    Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final Path root = Paths.get("images");

    private final ImageRepository imageRepository;
    private final ModelRepository modelRepository;
    private final ModelService modelService;
    public ImageService(ImageRepository imageRepository, ModelRepository modelRepository, ModelService modelService){
        this.imageRepository = imageRepository;
        this.modelRepository = modelRepository;
        this.modelService = modelService;
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
        Optional<Model> modelOpt = modelRepository.findById(modelNum);
        if (modelOpt.isEmpty()) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        } 

        Model model = modelOpt.get();
        ImagePrediction prediction = runPredictionOnModel(model, null);
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
        Optional<Model> modelOpt = modelRepository.findById(modelNum);
        if (modelOpt.isEmpty()) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        } 

        // download test set if not present
        ClassLoader cl = getClass().getClassLoader();
        URL resource = cl.getResource(root.toAbsolutePath().toString());
        Optional<File> fileOpt = Optional.empty();
        try {
            fileOpt = Files.walk(Paths.get(resource))
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(SAVED_MODEL_EXTENSION))
                    .map(x -> x.toFile())
                    .findAny();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }


        // find random sample in test set

        // if not in repository, run prediction and add to repository
        Model model = modelOpt.get();
        ImagePrediction prediction = runPredictionOnModel(model, null);
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
        Optional<Model> modelOpt = modelRepository.findById(modelNum);
        if (modelOpt.isEmpty()) {
            throw new Exception("Unable to run a prediction with model of number " + modelNum + " that is missing from the database");
        } 
        
        Model model = modelOpt.get();
        ImagePrediction prediction = runPredictionOnModel(model, null);
        imageRepository.save(prediction);
        return prediction;
    }


    private ImagePrediction runPredictionOnModel(Model m, File toPredict) {
        SavedModelBundle loadedModel = modelService.loadModelIntoTensorflow(m);
        
        return null;
    }
    
}
