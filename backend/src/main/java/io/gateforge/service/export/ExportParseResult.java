package io.gateforge.service.export;

import io.gateforge.model.ThreeScaleProduct;

import java.util.List;

public record ExportParseResult(
        List<ThreeScaleProduct> products,
        ExportBackendIndex backends,
        ExportManifest manifest) {}
