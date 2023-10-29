package com.jtrull.alzdetection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    
    public static final String DATASET_NAME = "Combined Dataset";
    public static final String ARCHIVE_FORMAT = ".zip";
    public static final String IMAGE_TYPE = ".jpg";

    
    public static final Path MODEL_ROOT = Paths.get("model");
    public static final Path IMAGE_ROOT = Paths.get("images");


     /**
     * 
     * @param path
     * @return
     */
    public static Long generateIdFromPath(String path) {
        return UUID.nameUUIDFromBytes(path.getBytes()).getMostSignificantBits();
    }

     /**
     * Return the path where images will be stored, represented in some way by /resource/images
     * 
     * @return
     */
    public static String returnImagePath() {
        return Utils.class.getResource("/").getPath() + IMAGE_ROOT;
    }

    /**
     * Return the path where models will be stored, represented in some way by /resource/model
     * 
     * @return
     */
    public static String returnModelPath() {
        return Utils.class.getResource("/").getPath() + MODEL_ROOT;
    }

    public static String testImagePath() {
        return returnImagePath() + "/" + DATASET_NAME + "/test/";
    }


    public static Logger getLogger() {
        return LOGGER;
    }

    public static String getDatasetName() {
        return DATASET_NAME;
    }

    public static String getArchiveFormat() {
        return ARCHIVE_FORMAT;
    }

    public static String getImageType() {
        return IMAGE_TYPE;
    }

    public static Path getModelRoot() {
        return MODEL_ROOT;
    }

    public static Path getImageRoot() {
        return IMAGE_ROOT;
    }
}

