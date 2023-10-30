package com.jtrull.alzdetection.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jtrull.alzdetection.exceptions.model.InvalidModelConfigurationException;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representation of the required files needed to make a Tensorflow model using Amazon DJL.
 */
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
    @JsonIgnore private String filepath;
    @JsonInclude(Include.NON_NULL) private String seabornPlotPath;
    @JsonInclude(Include.NON_NULL) private Integer accuracy;
    @JsonInclude(Include.NON_NULL) private Float loss;
    private String name;

    private static final Logger LOGGER = LoggerFactory.getLogger(Model.class); 

    public Model(){}

    public Model(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
    }

    /**
     * @return 
     * 
     */
    public Model findPlotForKey() {
        String key = name.substring(name.lastIndexOf("-") + 1, name.lastIndexOf("."));
        try {
            File plot = Files.walk(Paths.get(filepath))
                        .filter(Files::isRegularFile).map(r -> r.toFile())
                        .peek(r -> LOGGER.info(r.getName()))
                        .filter(r -> r.toString().contains("seaborn-plot-"))
                        .filter(r -> r.toString().contains(".png"))
                        .filter(r -> r.toString().contains(key))
                        .findFirst().orElse(null);
            if (plot != null) {
                this.seabornPlotPath = plot.getAbsolutePath();
            }
        } catch (IOException e) {
            throw new InvalidModelConfigurationException("Unable to process seaborn-plot file!");
        }
        return this;
    }

    /**
     * 
     */
    public Model findPropertiesForKey() {
        String key = name.substring(name.lastIndexOf("-") + 1, name.lastIndexOf("."));
        try {
            File properties = Files.walk(Paths.get(filepath))
                        .filter(Files::isRegularFile).map(r -> r.toFile())
                        .peek(r -> LOGGER.info(r.getName()))
                        .filter(r -> r.toString().contains("history-"))
                        .filter(r -> r.toString().contains(".properties"))
                        .filter(r -> r.toString().contains(key))
                        .findFirst().orElse(null);
            if (properties != null) {
                Properties p = new Properties();
                p.load(new FileInputStream(properties.getAbsolutePath()));

                this.loss = Float.valueOf(p.getProperty("loss"));
                this.accuracy = (int) (Float.valueOf(p.getProperty("acc")) * 100);
            }
        } catch (IOException e) {
            throw new InvalidModelConfigurationException("Unable to properties file!");
        }
        return this;
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
    public void setId(Long id) {
        this.id = id;
    }
    public String getSeabornPlotPath() {
        return seabornPlotPath;
    }
    public void setSeabornPlotPath(String seabornPlotPath) {
        this.seabornPlotPath = seabornPlotPath;
    }
    public Integer getAccuracy() {
        return accuracy;
    }
    public void setAccuracy(Integer accuracy) {
        this.accuracy = accuracy;
    }
    public Float getLoss() {
        return loss;
    }
    public void setLoss(Float loss) {
        this.loss = loss;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((filepath == null) ? 0 : filepath.hashCode());
        result = prime * result + ((seabornPlotPath == null) ? 0 : seabornPlotPath.hashCode());
        result = prime * result + ((accuracy == null) ? 0 : accuracy.hashCode());
        result = prime * result + ((loss == null) ? 0 : loss.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Model [id=");
        builder.append(id);
        builder.append(", filepath=");
        builder.append(filepath);
        builder.append(", seabornPlotPath=");
        builder.append(seabornPlotPath);
        builder.append(", accuracy=");
        builder.append(accuracy);
        builder.append(", loss=");
        builder.append(loss);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
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
        if (seabornPlotPath == null) {
            if (other.seabornPlotPath != null)
                return false;
        } else if (!seabornPlotPath.equals(other.seabornPlotPath))
            return false;
        if (accuracy == null) {
            if (other.accuracy != null)
                return false;
        } else if (!accuracy.equals(other.accuracy))
            return false;
        if (loss == null) {
            if (other.loss != null)
                return false;
        } else if (!loss.equals(other.loss))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }     
}
