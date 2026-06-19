package io.gateforge.resource;

import io.gateforge.model.ClusterFeatures;
import io.gateforge.model.ClusterReadiness;
import io.gateforge.model.ProjectInfo;
import io.gateforge.model.TargetCluster;
import io.gateforge.model.TargetClusterView;
import io.gateforge.service.ClusterReadinessService;
import io.gateforge.service.ClusterRegistry;
import io.gateforge.service.ClusterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

@Path("/api/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource {

    @Inject
    ClusterService clusterService;

    @Inject
    ClusterRegistry clusterRegistry;

    @Inject
    ClusterReadinessService clusterReadinessService;

    @ConfigProperty(name = "gateforge.developer-hub.enabled", defaultValue = "false")
    boolean developerHubEnabled;

    @ConfigProperty(name = "gateforge.developer-hub.url", defaultValue = "none")
    String developerHubUrl;

    @GET
    @Path("/projects")
    public List<ProjectInfo> listProjects() {
        return clusterService.listProjects();
    }

    @GET
    @Path("/projects/{name}")
    public ProjectInfo getProject(@PathParam("name") String name) {
        ProjectInfo project = clusterService.getProject(name);
        if (project == null) {
            throw new NotFoundException("Project not found: " + name);
        }
        return project;
    }

    @GET
    @Path("/features")
    public ClusterFeatures getFeatures() {
        String url = "none".equals(developerHubUrl) ? "" : developerHubUrl;
        return new ClusterFeatures(new ClusterFeatures.DeveloperHubFeature(developerHubEnabled, url));
    }

    @GET
    @Path("/targets")
    public List<TargetClusterView> listTargetClusters() {
        return clusterRegistry.listClusters().stream()
                .map(TargetClusterView::from)
                .toList();
    }

    @POST
    @Path("/targets")
    public TargetClusterView addTargetCluster(TargetCluster cluster) {
        clusterRegistry.addCluster(cluster);
        return TargetClusterView.from(cluster);
    }

    @DELETE
    @Path("/targets/{id}")
    public void removeTargetCluster(@PathParam("id") String id) {
        if ("local".equals(id)) {
            throw new BadRequestException("Cannot remove local cluster");
        }
        clusterRegistry.removeCluster(id);
    }

    @GET
    @Path("/targets/{id}/validate")
    public Map<String, Object> validateTargetCluster(@PathParam("id") String id) {
        return clusterRegistry.validateAccess(id);
    }

    @GET
    @Path("/readiness")
    public ClusterReadiness getReadiness(
            @QueryParam("targetClusterId") String targetClusterId,
            @QueryParam("planId") String planId) {
        return clusterReadinessService.probe(targetClusterId, planId);
    }
}
