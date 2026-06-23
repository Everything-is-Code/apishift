package io.apishift.model;

public record MigrationPrerequisite(
    String id,
    String category,
    String title,
    String description,
    boolean requiredByPlan,
    boolean optionalTier,
    String docUrl,
    String status,
    int triggeredByCount
) {
    public MigrationPrerequisite withStatus(String newStatus) {
        return new MigrationPrerequisite(
                id, category, title, description, requiredByPlan, optionalTier,
                docUrl, newStatus, triggeredByCount);
    }

    public MigrationPrerequisite withTriggeredByCount(int count) {
        return new MigrationPrerequisite(
                id, category, title, description, requiredByPlan, optionalTier,
                docUrl, status, count);
    }
}
