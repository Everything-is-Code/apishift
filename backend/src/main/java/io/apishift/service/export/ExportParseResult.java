package io.apishift.service.export;

import io.apishift.model.ThreeScaleProduct;

import java.util.List;

public record ExportParseResult(
        List<ThreeScaleProduct> products,
        ExportBackendIndex backends,
        ExportManifest manifest) {}
