package io.apishift.resource;

import io.apishift.model.ClusterFeatures;
import io.apishift.model.ClusterReadiness;
import io.apishift.model.ProjectInfo;
import io.apishift.model.TargetCluster;
import io.apishift.model.TargetClusterView;
import io.apishift.service.ClusterReadinessService;
import io.apishift.service.ClusterRegistry;
import io.apishift.service.ClusterService;
import io.apishift.security.ApiShiftRoles;
import jakarta.annotation.security.RolesAllowed;
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

    @ConfigProperty(name = "apishift.developer-hub.enabled", defaultValue = "false")
    boolean developerHubEnabled;

    @ConfigProperty(name = "apishift.developer-hub.url", defaultValue = "none")
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
    @RolesAllowed(ApiShiftRoles.ADMIN)
    public TargetClusterView addTargetCluster(TargetCluster cluster) {
        clusterRegistry.addCluster(cluster);
        return TargetClusterView.from(cluster);
    }

    @DELETE
    @Path("/targets/{id}")
    @RolesAllowed(ApiShiftRoles.ADMIN)
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
