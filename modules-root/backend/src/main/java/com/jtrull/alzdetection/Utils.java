package com.jtrull.alzdetection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jtrull.alzdetection.Prediction.ImpairmentEnum;
import com.jtrull.alzdetection.exceptions.generic.FailedRequirementException;

@Component
public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    HashMap<ImpairmentEnum, List<File>> testFiles = new HashMap<>();

    
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
    public String returnImagePath() {
        return getClass().getResource("/").getPath() + IMAGE_ROOT;
    }

    /**
     * Return the path where models will be stored, represented in some way by /resource/model
     * 
     * @return
     */
    public String returnModelPath() {
        return getClass().getResource("/").getPath() + MODEL_ROOT;
    }

    public String testImagePath() {
        return returnImagePath() + "/" + DATASET_NAME + "/test/";
    }

    /**
     * Given the Dataset .zip that is supplied with this application, populate the map of test images per impairment category. 
     *  These images are used in the /random prediction endpoint to supply some dummy MRI data
     */
    public void initializeTestImages() {

        Path p;
        try {
            p = Files.createDirectories(Paths.get(returnImagePath()));
        } catch (Exception ex) {
            throw new FailedRequirementException(ex, 
                "Could not create the directory where uploaded model files will be stored.");
        }

        // Find the Zipped dataset and unzip if found
        LOGGER.trace("searching for " + DATASET_NAME + ARCHIVE_FORMAT + " in directory: " + p);
        List<File> foundZipFiles = new ArrayList<>();
        try {
            foundZipFiles = Files.walk(p)
                    .filter(Files::isRegularFile)
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME + ARCHIVE_FORMAT))
                    .map(x -> x.toFile())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FailedRequirementException(e, "Unable to find dataset with name '" + DATASET_NAME + ARCHIVE_FORMAT + "'");
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
                    .filter(r -> r.getFileName().toString().contains(DATASET_NAME))
                    .map(x -> x.toFile())
                    .findFirst()
                    .orElseThrow(() -> 
                        new FailedRequirementException(new NoSuchElementException(), "Unable to find unzipped dataset with name '" + DATASET_NAME + "'"));

        } catch (IOException e) {
            throw new FailedRequirementException(e, "Unable to find unzipped dataset with name '" + DATASET_NAME + "'");
        }

        LOGGER.trace("found unzipped combined dataset directory: " + foundCombinedDatasetDir);

        // populate testFiles hashmap of all test images mapped to their corresponding categories
        List<File> impairmentCategories = null;
        Path testDirPath = Paths.get(testImagePath());
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
                        .filter(r -> r.getFileName().toString().contains(IMAGE_TYPE))
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
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public HashMap<ImpairmentEnum, List<File>> getTestFiles() {
        return testFiles;
    }

    public void setTestFiles(HashMap<ImpairmentEnum, List<File>> testFiles) {
        this.testFiles = testFiles;
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

     // public class MRIImageDataset extends RandomAccessDataset {
    //     private final List<MRIImage> images;

    //     private MRIImageDataset(Builder builder) {
    //         super(builder);
    //         this.images = builder.images;
    //     }

    //     @Override
    //     public void prepare(Progress progress) throws IOException, TranslateException {
    //         // TODO Auto-generated method stub
    //         throw new UnsupportedOperationException("Unimplemented method 'prepare'");
    //     }

    //     @Override
    //     public Record get(NDManager manager, long index) throws IOException {
    //         // TODO Auto-generated method stub
    //         throw new UnsupportedOperationException("Unimplemented method 'get'");

    //         MRIImage record = images.get(Math.toIntExact(index));
    //         NDArray datum = manager.create(encode(record.getImg()));
    //         NDArray label = manager.create(Float.parseFloat(record.getLabel()));
    //         return new Record(new NDList(datum), new NDList(label));
    //     }

    //     @Override
    //     protected long availableSize() {
    //         return images.size();
    //     }

    //     class MRIImage {
    //         private final Image img;
    //         private final ImpairmentEnum label;
            
    //         MRIImage(Image img, ImpairmentEnum label) {
    //             this.img = img;
    //             this.label = label;
    //         }

    //         public Image getImg() {
    //             return img;
    //         }
    //         public ImpairmentEnum getLabel() {
    //             return label;
    //         }
    //     }

    //     class Builder extends BaseBuilder<Builder> {
    //         List<Image> images;

    //         @Override
    //         protected Builder self() {
    //             return this;
    //         }

    //         MRIImageDataset build() throws IOException {
    //             if (testFiles.size() == 0) { 
    //                 initializeTestImages();
    //             }

    //             this.images = testFiles.values()
    //                 .stream()
    //                 .flatMap(List::stream)
    //                 .map(f -> {
    //                     try {
    //                         return ImageFactory.getInstance().fromFile(f.toPath());
    //                     } catch (IOException e) {
    //                         //TOOD:
    //                         throw new RuntimeException();
    //                     }
    //                 })
    //                 .collect(Collectors.toList());

                
    //             return new MRIImageDataset(this);
    //         }
    //     }
    // }
}

