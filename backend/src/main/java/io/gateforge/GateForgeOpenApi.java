package io.gateforge;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@ApplicationScoped
@OpenAPIDefinition(
        info = @Info(
                title = "GateForge API",
                version = "0.2.0",
                description = "3scale to Connectivity Link migration console REST API"))
public class GateForgeOpenApi {
}
