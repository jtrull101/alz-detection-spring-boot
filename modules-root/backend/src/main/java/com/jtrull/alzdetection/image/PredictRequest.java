package com.jtrull.alzdetection.image;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;

public class PredictRequest {
    private Image image;
    private Thread caller;
    private Classifications result;

    public PredictRequest(Image image, Thread caller) {
        this.image = image;
        this.caller = caller;
    }
    public Image getImage() {
        return image;
    }
    public void setImage(Image image) {
        this.image = image;
    }
    public Thread getCaller() {
        return caller;
    }
    public void setCaller(Thread caller) {
        this.caller = caller;
    }
    public Classifications getResult() {
        return result;
    }
    public void setResult(Classifications result) {
        this.result = result;
    }
    
}