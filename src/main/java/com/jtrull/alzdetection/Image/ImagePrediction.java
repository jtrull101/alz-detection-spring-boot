package com.jtrull.alzdetection.Image;

import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table
public class ImagePrediction {
    @Id
    @SequenceGenerator(
        name ="image_sequence",
        sequenceName = "image_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "image_sequence"
    )
    private Long id;
    private String filepath;
    private Integer confidenceNone;
    private Integer confidenceVeryMild;
    private Integer confidenceMild;
    private Integer confidenceModerate;
    private ImpairmentEnum actualValue;
    private Long associatedModel;

    public ImagePrediction(Long id, String filepath, Integer confidenceNone, Integer confidenceVeryMild,
            Integer confidenceMild, Integer confidenceModerate, ImpairmentEnum actualValue, Long associatedModel) {
        this.id = id;
        this.filepath = filepath;
        this.confidenceNone = confidenceNone;
        this.confidenceVeryMild = confidenceVeryMild;
        this.confidenceMild = confidenceMild;
        this.confidenceModerate = confidenceModerate;
        this.actualValue = actualValue;
        this.associatedModel = associatedModel;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public ImpairmentEnum getActualValue() {
        return actualValue;
    }
    public void setActualValue(ImpairmentEnum actualValue) {
        this.actualValue = actualValue;
    }
    public Integer getConfidenceNone() {
        return confidenceNone;
    }
    public void setConfidenceNone(Integer confidenceNone) {
        this.confidenceNone = confidenceNone;
    }
    public Integer getConfidenceVeryMild() {
        return confidenceVeryMild;
    }
    public void setConfidenceVeryMild(Integer confidenceVeryMild) {
        this.confidenceVeryMild = confidenceVeryMild;
    }
    public Integer getConfidenceMild() {
        return confidenceMild;
    }
    public void setConfidenceMild(Integer confidenceMild) {
        this.confidenceMild = confidenceMild;
    }
    public Integer getConfidenceModerate() {
        return confidenceModerate;
    }
    public void setConfidenceModerate(Integer confidenceModerate) {
        this.confidenceModerate = confidenceModerate;
    }
    public Long getAssociatedModel() {
        return associatedModel;
    }
    public void setAssociatedModel(Long associatedModel) {
        this.associatedModel = associatedModel;
    }
}
