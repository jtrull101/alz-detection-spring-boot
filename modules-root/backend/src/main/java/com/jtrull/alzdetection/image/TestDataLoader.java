package com.jtrull.alzdetection.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jtrull.alzdetection.Utils;
import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;
import com.jtrull.alzdetection.prediction.ImpairmentEnum;


public class TestDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataLoader.class);
    
    private static volatile TestDataLoader INSTANCE;
    private LinkedHashMap<ImpairmentEnum, List<File>> testFiles;

    private TestDataLoader() {}

    public synchronized static TestDataLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestDataLoader();
        }
        return INSTANCE;
    }

    public LinkedHashMap<ImpairmentEnum, List<File>> getTestFiles() {
        if (testFiles == null) {
            testFiles = initializeTestImages();
        }
        return testFiles;
    }


    /**
     * Given the Dataset .zip that is supplied with this application, populate the map of test images per impairment category. 
     *  These images are used in the /random prediction endpoint to supply some dummy MRI data
     * @return 
     */
    public static LinkedHashMap<ImpairmentEnum, List<File>> initializeTestImages() {
        LinkedHashMap<ImpairmentEnum, List<File>> testFiles = new LinkedHashMap<>();

        Path p;
        try {
            p = Files.createDirectories(Paths.get(Utils.returnImagePath()));
        } catch (Exception ex) {
            throw new FailedRequirementException(ex, 
                "Could not create the directory where uploaded model files will be stored.");
        }

        // Find the Zipped dataset and unzip if found
        LOGGER.trace("searching for " + Utils.DATASET_NAME + Utils.ARCHIVE_FORMAT + " in directory: " + p);
        List<File> foundZipFiles = new ArrayList<>();
        try {
            foundZipFiles = Files.walk(p)
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(Utils.DATASET_NAME + Utils.ARCHIVE_FORMAT))
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FailedRequirementException(e, "Unable to find dataset with name '" + Utils.DATASET_NAME + Utils.ARCHIVE_FORMAT + "'");
        }

        LOGGER.trace("found zipped combined dataset directory: " + foundZipFiles);
        for (File f : foundZipFiles) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(f.toPath()))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    final Path toPath = p.resolve(entry.getName());
                    if (toPath.toString().contains("/train")) {
                        // ignore training data
                        continue;
                    }
                    // create directories for nested zip
                    if (entry.isDirectory()) {
                        if (!toPath.toFile().exists()) {
                            Files.createDirectory(toPath);
                        }
                    } else {
                        if (!toPath.toFile().exists()) {
                            Files.copy(zipInputStream, toPath);
                        }
                    }
                }
            } catch (IOException e) {
                throw new FailedRequirementException(e, "Error while decompressing dataset archive");
            }
        }

        // Now the contents have been unzipped. Search for the uncompressed dataset
        File foundCombinedDatasetDir;
        try {
            foundCombinedDatasetDir = Files.walk(p)
                    .filter(Files::isDirectory)
                    .filter(r -> r.getFileName().toString().contains(Utils.DATASET_NAME))
                    .map(x -> x.toFile())
                    .findFirst()
                    .orElseThrow(() -> 
                        new FailedRequirementException(new NoSuchElementException(), "Unable to find unzipped dataset with name '" + Utils.DATASET_NAME + "'"));

        } catch (IOException e) {
            throw new FailedRequirementException(e, "Unable to find unzipped dataset with name '" + Utils.DATASET_NAME + "'");
        }

        LOGGER.trace("found unzipped combined dataset directory: " + foundCombinedDatasetDir);

        // populate testFiles hashmap of all test images mapped to their corresponding categories
        List<File> impairmentCategories = null;
        Path testDirPath = Paths.get(Utils.testImagePath());
        try {
            impairmentCategories = Files.walk(testDirPath)
                    .filter(Files::isDirectory)
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new FailedRequirementException(e, "Error while finding /test directory at location: '" + testDirPath + "'");
        }

        for (File f : impairmentCategories) {
            String name = f.getName().replaceAll("\\P{Print}", "");
            if (name.equals("test")) continue; // exclude parent directory
            
            LOGGER.trace("processing potential impairment category: " + name);
            ImpairmentEnum category = ImpairmentEnum.fromString(name);
            List<File> images = new ArrayList<>();
            try {
                images = Files.walk(f.toPath())
                        .filter(Files::isRegularFile)
                        .filter(r -> r.getFileName().toString().contains(Utils.IMAGE_TYPE))
                        .map(x -> x.toFile())
                        .collect(Collectors.toList());

            } catch (IOException e) {
                throw new FailedRequirementException(e, "Error while gathering images from category " + name);
            }

            if ((testFiles.containsKey(category) && testFiles.get(category).size() < images.size()) || !testFiles.containsKey(category)) {
                LOGGER.info("adding " + images.size() + " images to category: " + category);
                testFiles.put(category, images);
            }
        }
        return testFiles;
    }
    
}
