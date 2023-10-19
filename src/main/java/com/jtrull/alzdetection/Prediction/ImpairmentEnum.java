package com.jtrull.alzdetection.Prediction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Optional<ImpairmentEnum> fromString(String val) {
        return Stream.of(values())
            .filter(ie -> ie.toString().equals(val))
            .findFirst();
    }

    public static List<String> asStrings() {
        return Stream.of(values())
            .map(v -> v.toString())
            .collect(Collectors.toList());
    }
}
