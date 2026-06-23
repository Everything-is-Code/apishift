package io.apishift.model;

public record ProjectInfo(
    String name,
    String status,
    String creationTimestamp,
    boolean hasThreeScale,
    boolean hasKuadrant
) {}
