package io.gateforge.service.support;

import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.KuadrantCtlService;
import io.gateforge.service.ThreeScaleService;
import io.gateforge.service.ThreeScaleSourceRegistry;

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
