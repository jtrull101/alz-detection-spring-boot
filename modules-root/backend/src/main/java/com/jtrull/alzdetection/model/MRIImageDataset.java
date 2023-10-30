package com.jtrull.alzdetection.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.jtrull.alzdetection.image.TestDataLoader;
import com.jtrull.alzdetection.prediction.ImpairmentEnum;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.translate.TranslateException;
import ai.djl.util.Progress;
import ai.djl.training.dataset.Record;

public class MRIImageDataset extends RandomAccessDataset {
    private final List<MRIImage> images;

    public MRIImageDataset(MRIImageDataset.Builder builder) {
        super(builder);
        this.images = builder.images;
    }

    @Override
    public void prepare(Progress progress) throws IOException, TranslateException {
        throw new UnsupportedOperationException("Unimplemented method 'prepare'");
    }

    @Override
    public Record get(NDManager manager, long index) throws IOException {

        MRIImage mri = images.get(Math.toIntExact(index));

        // Convert image to pixel matrix
        BufferedImage bufferedImage = ImageIO.read(mri.getFile());
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[][] pixels = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[i][j] = bufferedImage.getRGB(i, j);
            }
        }
        NDArray datum = manager.create(pixels);
        NDArray label = manager.create(mri.getLabel().toString());
        return new Record(new NDList(datum), new NDList(label));
    }

    @Override
    protected long availableSize() {
        return images.size();
    }

    public static final class MRIImage {
        private final File file;
        private final ImpairmentEnum label;
        
        MRIImage(File file, ImpairmentEnum label) {
            this.file = file;
            this.label = label;
        }

        public File getFile() {
            return file;
        }
        public ImpairmentEnum getLabel() {
            return label;
        }
    }

    public static final class Builder extends BaseBuilder<MRIImageDataset.Builder> {
        List<MRIImage> images;

        @Override
        protected MRIImageDataset.Builder self() {
            return this;
        }

        public MRIImageDataset build() throws IOException {
            HashMap<ImpairmentEnum, List<File>> testFiles = TestDataLoader.getInstance().getTestFiles();

            this.images = testFiles.values()
                .stream()
                .flatMap(List::stream)
                .map(f -> new MRIImage(f, ImpairmentEnum.valueOf(f.getParentFile().getName())))
                .collect(Collectors.toList());

            return new MRIImageDataset(this);
        }
    }
}