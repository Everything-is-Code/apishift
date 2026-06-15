package io.gateforge.service.support;

import io.gateforge.model.ThreeScaleProduct;

public final class MinimalThreeScaleFixtures {

    private MinimalThreeScaleFixtures() {}

    public static ThreeScaleProduct minimalProduct() {
        return MigrationFixtures.apiKeyProduct();
    }
}
