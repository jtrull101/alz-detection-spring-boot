package com.jtrull.alzdetection.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    @JsonIgnore
    private String filepath;
    private String name;
    // private SavedModelBundle model;

    public Model(){}

    public Model(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((filepath == null) ? 0 : filepath.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Model other = (Model) obj;
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
