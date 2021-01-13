package io.quarkus.transactions;

import io.smallrye.health.api.HealthGroup;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@HealthGroup("custom")
public class CustomGroupLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("custom liveness");
    }
}