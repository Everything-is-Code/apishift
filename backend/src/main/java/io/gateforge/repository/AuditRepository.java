package io.gateforge.repository;

import io.gateforge.entity.AuditEntryEntity;
import io.gateforge.model.AuditEntry;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuditRepository implements PanacheRepositoryBase<AuditEntryEntity, String> {

    public List<AuditEntry> findAllDescending() {
        return listAll(Sort.by("timestamp").descending()).stream()
                .map(this::toAuditEntry)
                .collect(Collectors.toList());
    }

    @Transactional
    public void save(AuditEntry entry) {
        AuditEntryEntity e = new AuditEntryEntity();
        e.id = entry.id();
        e.timestamp = entry.timestamp();
        e.action = entry.action();
        e.resourceKind = entry.resourceKind();
        e.resourceName = entry.resourceName();
        e.namespace = entry.namespace();
        e.yamlBefore = entry.yamlBefore();
        e.yamlAfter = entry.yamlAfter();
        e.performedBy = entry.performedBy();
        e.targetClusterId = entry.targetClusterId();
        persist(e);
    }

    private AuditEntry toAuditEntry(AuditEntryEntity e) {
        return new AuditEntry(
                e.id, e.timestamp, e.action, e.resourceKind, e.resourceName,
                e.namespace, e.yamlBefore, e.yamlAfter, e.performedBy, e.targetClusterId
        );
    }
}
