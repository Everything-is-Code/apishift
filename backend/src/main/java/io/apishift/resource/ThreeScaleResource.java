package io.apishift.resource;

import io.apishift.model.ThreeScaleAdminStatus;
import io.apishift.model.ThreeScaleBackend;
import io.apishift.model.ThreeScaleProduct;
import io.apishift.model.ThreeScaleRefreshResult;
import io.apishift.model.ThreeScaleSource;
import io.apishift.model.ThreeScaleSourceStatus;
import io.apishift.model.ThreeScaleSourceView;
import io.apishift.service.ThreeScaleService;
import io.apishift.service.ThreeScaleSourceRegistry;
import io.apishift.security.ApiShiftRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/api/threescale")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ThreeScaleResource {

    @Inject
    ThreeScaleService threeScaleService;

    @Inject
    ThreeScaleSourceRegistry sourceRegistry;

    @GET
    @Path("/products")
    public List<ThreeScaleProduct> listProducts() {
        return threeScaleService.listProducts();
    }

    @GET
    @Path("/products/{namespace}/{name}")
    public ThreeScaleProduct getProduct(@PathParam("namespace") String namespace,
                                        @PathParam("name") String name) {
        ThreeScaleProduct product = threeScaleService.getProduct(name, namespace);
        if (product == null) {
            throw new NotFoundException("Product not found: " + name);
        }
        return product;
    }

    @GET
    @Path("/backends")
    public List<ThreeScaleBackend> listBackends() {
        return threeScaleService.listBackendsCombined().stream()
                .map(ThreeScaleBackend::fromMap)
                .toList();
    }

    @GET
    @Path("/status")
    public ThreeScaleAdminStatus getStatus() {
        return ThreeScaleAdminStatus.fromMap(threeScaleService.getAdminApiStatus());
    }

    @POST
    @Path("/refresh")
    @RolesAllowed({ApiShiftRoles.OPERATOR, ApiShiftRoles.ADMIN})
    public ThreeScaleRefreshResult refreshDiscovery() {
        return ThreeScaleRefreshResult.fromMap(threeScaleService.refreshDiscovery());
    }

    @GET
    @Path("/sources")
    public List<ThreeScaleSourceView> listSources() {
        return sourceRegistry.listSources().stream()
                .map(ThreeScaleSourceView::from)
                .toList();
    }

    @POST
    @Path("/sources")
    @RolesAllowed(ApiShiftRoles.ADMIN)
    public ThreeScaleSourceView addSource(ThreeScaleSource source) {
        sourceRegistry.addSource(source);
        return ThreeScaleSourceView.from(source);
    }

    @DELETE
    @Path("/sources/{id}")
    @RolesAllowed(ApiShiftRoles.ADMIN)
    public void removeSource(@PathParam("id") String id) {
        if ("default".equals(id)) {
            throw new BadRequestException("Cannot remove default source");
        }
        sourceRegistry.removeSource(id);
    }

    @GET
    @Path("/sources/{id}/status")
    public ThreeScaleSourceStatus getSourceStatus(@PathParam("id") String id) {
        return ThreeScaleSourceStatus.fromMap(sourceRegistry.getSourceStatus(id));
    }
}
