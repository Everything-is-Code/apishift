package io.apishift.service.support;

import io.apishift.model.ThreeScaleProduct;

public final class MinimalThreeScaleFixtures {

    private MinimalThreeScaleFixtures() {}

    public static ThreeScaleProduct minimalProduct() {
        return MigrationFixtures.apiKeyProduct();
    }
}
