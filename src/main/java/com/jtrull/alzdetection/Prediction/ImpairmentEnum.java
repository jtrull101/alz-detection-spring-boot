package com.jtrull.alzdetection.Prediction;

public enum ImpairmentEnum {
    NO_IMPAIRMENT("No Impairment"),
    VERY_MILD_IMPAIRMENT("Very Mild Impairment"),
    MILD_IMPAIRMENT("Mild Impairment"),
    MODERATE_IMPAIRMENT("Moderate Impairment");

    private String val;

    ImpairmentEnum(String val) {
        this.val = val;
    }

    public String toString() {
        return this.val;
    }
}
