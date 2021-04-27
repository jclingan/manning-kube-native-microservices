package io.quarkus.transactions;


import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.math.BigDecimal;

@Readiness
public class AccountHealthReadinessCheck implements HealthCheck {
    @Inject
    @RestClient
    AccountService accountService;

    BigDecimal balance;

    @Override
    public HealthCheckResponse call() {
        try {
            balance = accountService.getBalance(999999999L);
        } catch (WebApplicationException ex) {
            // This class is a singleton, so clear last request's balance
            balance = new BigDecimal(Integer.MIN_VALUE);

            if (ex.getResponse().getStatus() >= 500) {
                return HealthCheckResponse
                        .named("AccountServiceCheck")
                        .withData("exception", ex.toString())
                        .down()
                        .build();
            }
        }

        return HealthCheckResponse
                .named("AccountServiceCheck")
                .withData("balance", balance.toString())
                .up()
                .build();
    }
}