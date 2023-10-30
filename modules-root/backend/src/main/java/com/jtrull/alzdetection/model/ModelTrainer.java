package com.jtrull.alzdetection.model;

import java.io.IOException;

import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.translate.TranslateException;

public class ModelTrainer {
    
    public ModelTrainer(){}

    /**
     * TODO: Not currently implemented. Training the model might be nice in the future
     * @param inMemModel
     */
    public static void trainModelOnTestData(InMemoryModel inMemModel) {
        ai.djl.Model djlModel = inMemModel.getLoadedModel().getWrappedModel();
        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.maskedSoftmaxCrossEntropyLoss())
            .addEvaluator(new Accuracy())
            .addTrainingListeners(TrainingListener.Defaults.logging());
            
        Trainer trainer = null;
        try{
            trainer = djlModel.newTrainer(config);
        } catch (Exception e) {
            System.out.println(e);
        }
        

        int batchSize = 32;
        trainer.initialize(new Shape(batchSize,3,128,128));

        MRIImageDataset dataset;
        try {
            dataset = new MRIImageDataset.Builder().build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            EasyTrain.evaluateDataset(trainer, dataset);
        } catch (IOException | TranslateException e) {
            throw new RuntimeException(e);
        }
    }
}
