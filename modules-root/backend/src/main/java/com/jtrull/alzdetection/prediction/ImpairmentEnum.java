package com.jtrull.alzdetection.prediction;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jtrull.alzdetection.exceptions.predictions.InvalidImpairmentCategoryException;

public enum ImpairmentEnum {
    MILD_IMPAIRMENT("Mild Impairment"),
    NO_IMPAIRMENT("No Impairment"),
    MODERATE_IMPAIRMENT("Moderate Impairment"),
    VERY_MILD_IMPAIRMENT("Very Mild Impairment");

    private String val;

    ImpairmentEnum(String val) {
        this.val = val;
    }

    public String toString() {
        return this.val;
    }

    public static ImpairmentEnum fromString(String val) {
        return Stream.of(values())
            .filter(ie -> ie.toString().equals(val))
            .findFirst()
            .orElseThrow(() -> new InvalidImpairmentCategoryException(val));
    }

    public static List<String> asStrings() {
        return Stream.of(values())
            .map(v -> v.toString())
            .collect(Collectors.toList());
    }
}
