package com.jtrull.alzdetection.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table
public class Model {
    @Id
    @SequenceGenerator(
        name = "model_sequence",
        sequenceName = "model_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "model_sequence"
    )
    private Long id;
    private String filepath;
    // private SavedModelBundle model;

    public Model(){}

    public Model(Long id, String filepath) {
        this.id = id;
        this.filepath = filepath;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Model [id=");
        builder.append(id);
        builder.append(", filepath=");
        builder.append(filepath);
        builder.append("]");
        return builder.toString();
    }

}
