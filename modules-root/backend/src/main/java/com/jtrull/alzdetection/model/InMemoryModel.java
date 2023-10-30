package com.jtrull.alzdetection.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.jtrull.alzdetection.exceptions.model.InvalidModelFileException;
import com.jtrull.alzdetection.exceptions.predictions.PredictionFailureException;
import com.jtrull.alzdetection.image.ImagePrediction;
import com.jtrull.alzdetection.prediction.ImpairmentEnum;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

public class InMemoryModel {
    private final ModelService modelService;
    private Long id;
    private Criteria<Image, Classifications> criteria;
    private ZooModel<Image, Classifications> loadedModel;
    private Predictor<Image, Classifications> predictor;

    // TODO: unused
    // private Queue<PredictRequest> requests = new ArrayDeque<PredictRequest>();
    // private boolean running = true;

    /**
     * 
     * @param modelService
     * @param id
     * @param criteria
     */
    public InMemoryModel(ModelService modelService, Long id, Criteria<Image, Classifications> criteria) {
        this.modelService = modelService;
        this.id = id;
        this.criteria = criteria;
        try {
            this.loadedModel = ModelZoo.loadModel(criteria);
        } catch (IOException | MalformedModelException e) {
            throw new InvalidModelFileException(criteria, "Error while loading model: '" + criteria + "'");
            
        } catch (ModelNotFoundException e) {
            throw new InvalidModelFileException(criteria, "Unable to find valid model for criteria: '" + criteria + "'");
        }
        this.predictor = this.loadedModel.newPredictor();
    }

    /**
     * 
     * @param file
     * @return
     */
    public Image fileToImage(File file) {
        try {
            return ImageFactory.getInstance().fromFile(file.toPath());
        } catch (IOException e) {
            throw new PredictionFailureException(file, "Error while loading image " + file.toPath() + " is it a valid image?");
        }
    }

    /**
     * 
     * @param file
     * @return
     */
    public ImagePrediction predictOnModel(File file) {
        Image image = fileToImage(file);
        try {
            return this.modelService.convertClassificationsToPrediction(predictor.predict(image), file, null, this.getId());
        } catch (TranslateException e) {
            throw new PredictionFailureException(file, "Error during translation of file: '" + criteria + "'");
        }
    }

    /**
     * 
     * @param file
     * @param actualImpairmentValue
     * @return
     */
    public ImagePrediction predictOnModel(File file, ImpairmentEnum actualImpairmentValue) {
        Image image = fileToImage(file);
        try {
            return this.modelService.convertClassificationsToPrediction(predictor.predict(image), file, actualImpairmentValue, this.getId());
        } catch (TranslateException e) {
            throw new PredictionFailureException(file, "Error during translation of file: '" + criteria + "'");
        }
    }

    /**
     * 
     * @param files
     * @param actualImpairmentValue
     * @return
     */
    public List<ImagePrediction> batchPredict(List<File> files, ImpairmentEnum actualImpairmentValue) {
        List<Image> images = files.stream()
            .map(f -> fileToImage(f))
            .collect(Collectors.toList());

       try {
            List<Classifications> classifications = predictor.batchPredict(images);
            return classifications.stream()
                .map(c-> this.modelService.convertClassificationsToPrediction(c, files.get(classifications.indexOf(c)), actualImpairmentValue, this.getId()))
                .collect(Collectors.toList());
        } catch (TranslateException e) {
            throw new PredictionFailureException(files, "Error during batch translation of files: '" + criteria + "'");
        }
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Criteria<Image, Classifications> getCriteria() {
        return criteria;
    }
    public void setCriteria(Criteria<Image, Classifications> criteria) {
        this.criteria = criteria;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public ZooModel<Image, Classifications> getLoadedModel() {
        return loadedModel;
    }

    public void setLoadedModel(ZooModel<Image, Classifications> loadedModel) {
        this.loadedModel = loadedModel;
    }

    public Predictor<Image, Classifications> getPredictor() {
        return predictor;
    }

    public void setPredictor(Predictor<Image, Classifications> predictor) {
        this.predictor = predictor;
    }
}