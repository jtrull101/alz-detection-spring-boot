package com.jtrull.alzdetection.Image;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    @JsonIgnore
    private String filepath;
    private Integer conf_NoImpairment;
    private Integer conf_VeryMildImpairment;
    private Integer conf_MildImpairment;
    private Integer conf_ModerateImpairment;
    @JsonInclude(Include.NON_NULL)
    private ImpairmentEnum actualValue;
    @JsonIgnore
    private Long associatedModel;

    public ImagePrediction(){}
    
    public ImagePrediction(String filepath, Integer confidenceNone, Integer confidenceVeryMild,
            Integer confidenceMild, Integer confidenceModerate, ImpairmentEnum actualValue, Long associatedModel) {
        this.filepath = filepath;
        this.actualValue = actualValue;
        this.associatedModel = associatedModel;
        this.conf_NoImpairment = confidenceNone;
        this.conf_VeryMildImpairment = confidenceVeryMild;
        this.conf_MildImpairment = confidenceMild;
        this.conf_ModerateImpairment = confidenceModerate;
    }

    public Long getId() {
        return id;
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
    public Long getAssociatedModel() {
        return associatedModel;
    }
    public void setAssociatedModel(Long associatedModel) {
        this.associatedModel = associatedModel;
    }

    public Integer getConf_NoImpairment() {
        return conf_NoImpairment;
    }

    public void setConf_NoImpairment(Integer conf_NoImpairment) {
        this.conf_NoImpairment = conf_NoImpairment;
    }

    public Integer getConf_VeryMildImpairment() {
        return conf_VeryMildImpairment;
    }

    public void setConf_VeryMildImpairment(Integer conf_VeryMildImpairment) {
        this.conf_VeryMildImpairment = conf_VeryMildImpairment;
    }

    public Integer getConf_MildImpairment() {
        return conf_MildImpairment;
    }

    public void setConf_MildImpairment(Integer conf_MildImpairment) {
        this.conf_MildImpairment = conf_MildImpairment;
    }

    public Integer getConf_ModerateImpairment() {
        return conf_ModerateImpairment;
    }

    public void setConf_ModerateImpairment(Integer conf_ModerateImpairment) {
        this.conf_ModerateImpairment = conf_ModerateImpairment;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ImagePrediction [id=");
        builder.append(id);
        builder.append(", filepath=");
        builder.append(filepath);
        builder.append(", conf_NoImpairment=");
        builder.append(conf_NoImpairment);
        builder.append(", conf_VeryMildImpairment=");
        builder.append(conf_VeryMildImpairment);
        builder.append(", conf_MildImpairment=");
        builder.append(conf_MildImpairment);
        builder.append(", conf_ModerateImpairment=");
        builder.append(conf_ModerateImpairment);
        builder.append(", actualValue=");
        builder.append(actualValue);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((filepath == null) ? 0 : filepath.hashCode());
        result = prime * result + ((conf_NoImpairment == null) ? 0 : conf_NoImpairment.hashCode());
        result = prime * result + ((conf_VeryMildImpairment == null) ? 0 : conf_VeryMildImpairment.hashCode());
        result = prime * result + ((conf_MildImpairment == null) ? 0 : conf_MildImpairment.hashCode());
        result = prime * result + ((conf_ModerateImpairment == null) ? 0 : conf_ModerateImpairment.hashCode());
        result = prime * result + ((actualValue == null) ? 0 : actualValue.hashCode());
        result = prime * result + ((associatedModel == null) ? 0 : associatedModel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImagePrediction other = (ImagePrediction) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (filepath == null) {
            if (other.filepath != null)
                return false;
        } else if (!filepath.equals(other.filepath))
            return false;
        if (conf_NoImpairment == null) {
            if (other.conf_NoImpairment != null)
                return false;
        } else if (!conf_NoImpairment.equals(other.conf_NoImpairment))
            return false;
        if (conf_VeryMildImpairment == null) {
            if (other.conf_VeryMildImpairment != null)
                return false;
        } else if (!conf_VeryMildImpairment.equals(other.conf_VeryMildImpairment))
            return false;
        if (conf_MildImpairment == null) {
            if (other.conf_MildImpairment != null)
                return false;
        } else if (!conf_MildImpairment.equals(other.conf_MildImpairment))
            return false;
        if (conf_ModerateImpairment == null) {
            if (other.conf_ModerateImpairment != null)
                return false;
        } else if (!conf_ModerateImpairment.equals(other.conf_ModerateImpairment))
            return false;
        if (actualValue != other.actualValue)
            return false;
        if (associatedModel == null) {
            if (other.associatedModel != null)
                return false;
        } else if (!associatedModel.equals(other.associatedModel))
            return false;
        return true;
    }
}

