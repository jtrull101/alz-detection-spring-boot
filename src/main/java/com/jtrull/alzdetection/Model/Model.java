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
    private String name;
    // private SavedModelBundle model;

    public Model(){}

    public Model(Long id, String filepath, String name) {
        this.id = id;
        this.filepath = filepath;
        this.name = name;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Model [id=");
        builder.append(id);
        builder.append(", filepath=");
        builder.append(filepath);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
