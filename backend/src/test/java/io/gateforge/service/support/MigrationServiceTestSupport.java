package io.gateforge.service.support;

import io.gateforge.model.MigrationPlan;

import java.util.List;

public abstract class MigrationServiceTestSupport {

    protected static boolean hasKind(MigrationPlan plan, String kind) {
        return plan.resources().stream().anyMatch(r -> kind.equals(r.kind()));
    }

    protected static long countKind(MigrationPlan plan, String kind) {
        return plan.resources().stream().filter(r -> kind.equals(r.kind())).count();
    }

    protected static List<String> resourceNames(MigrationPlan plan, String kind) {
        return plan.resources().stream()
                .filter(r -> kind.equals(r.kind()))
                .map(MigrationPlan.GeneratedResource::name)
                .toList();
    }

    protected static boolean warningsContain(MigrationPlan plan, String substring) {
        if (plan.consolidationWarnings() == null) {
            return false;
        }
        return plan.consolidationWarnings().stream()
                .anyMatch(w -> w.contains(substring));
    }

    protected static String resourceYaml(MigrationPlan plan, String kind) {
        return plan.resources().stream()
                .filter(r -> kind.equals(r.kind()))
                .findFirst()
                .map(MigrationPlan.GeneratedResource::yaml)
                .orElse("");
    }
}
