package com.jtrull.alzdetection.Image;

import ai.djl.ndarray.NDList;

public class MRIRecord {
    private NDList datum;
    private NDList label;
    
    public MRIRecord(NDList datum, NDList label) {
        this.datum = datum;
        this.label = label;
    }

    public NDList getDatum() {
        return datum;
    }
    public void setDatum(NDList datum) {
        this.datum = datum;
    }
    public NDList getLabel() {
        return label;
    }
    public void setLabel(NDList label) {
        this.label = label;
    }
}
