package io.apishift.service.support;

import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.KuadrantCtlService;
import io.apishift.service.ThreeScaleService;
import io.apishift.service.ThreeScaleSourceRegistry;

import java.util.List;

public final class TestDoubles {

    private TestDoubles() {}

    public static ThreeScaleService threeScaleService(List<ThreeScaleProduct> products) {
        return new ThreeScaleService() {
            @Override
            public List<ThreeScaleProduct> listProducts() {
                return products;
            }

            @Override
            public ThreeScaleProduct refreshProductForMigration(ThreeScaleProduct product) {
                return product;
            }
        };
    }

    public static ThreeScaleSourceRegistry emptySourceRegistry() {
        return new ThreeScaleSourceRegistry() {
            @Override
            public boolean hasConfiguredClients() {
                return false;
            }
        };
    }

    public static KuadrantCtlService failingKuadrantCtl() {
        return new KuadrantCtlService() {
            @Override
            public String generateHttpRoute(String oasContent) {
                return "ERROR";
            }
        };
    }
}
