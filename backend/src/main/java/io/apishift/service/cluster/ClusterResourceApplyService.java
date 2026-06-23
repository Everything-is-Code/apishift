package io.apishift.service.cluster;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.apishift.model.DriftEntry;
import io.apishift.model.MigrationPlan;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies, reverts, and checks drift for generated Kubernetes resources on a target cluster.
 */
@ApplicationScoped
public class ClusterResourceApplyService {

    private static final Logger LOG = Logger.getLogger(ClusterResourceApplyService.class);

    public void applyYaml(KubernetesClient client, String yaml, String namespace) {
        GenericKubernetesResource generic = unmarshal(yaml, namespace);
        String apiVersion = generic.getApiVersion();
        if (apiVersion != null && apiVersion.contains("route.openshift.io")) {
            client.resource(generic).createOr(r -> r.update());
        } else {
            client.resource(generic).serverSideApply();
        }
    }

    public void deleteResource(KubernetesClient client, String yaml, String namespace) {
        client.resource(unmarshal(yaml, namespace)).delete();
    }

    public DriftEntry checkDrift(KubernetesClient client, MigrationPlan.GeneratedResource res) {
        String status;
        String message = null;
        try {
            GenericKubernetesResource generic = unmarshal(res.yaml(), res.namespace());
            var existing = client.resource(generic).get();
            status = existing != null ? "in-sync" : "missing";
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("NotFound")) {
                status = "missing";
            } else {
                status = "error";
                message = msg;
                LOG.debugf("Drift check error for %s/%s: %s", res.kind(), res.name(), msg);
            }
        }
        return new DriftEntry(res.kind(), res.name(), res.namespace(), status, message);
    }

    public List<DriftEntry> checkDrift(KubernetesClient client, List<MigrationPlan.GeneratedResource> resources) {
        List<DriftEntry> driftReport = new ArrayList<>(resources.size());
        for (MigrationPlan.GeneratedResource res : resources) {
            driftReport.add(checkDrift(client, res));
        }
        return driftReport;
    }

    private static GenericKubernetesResource unmarshal(String yaml, String namespace) {
        GenericKubernetesResource generic = Serialization.unmarshal(yaml, GenericKubernetesResource.class);
        if (namespace != null && !namespace.isBlank()) {
            generic.getMetadata().setNamespace(namespace);
        }
        return generic;
    }
}
