package io.quarkus.transactions;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletionStage;

@Path("/transactions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {
  @Inject
  @RestClient
  AccountService accountService;

  @ConfigProperty(name = "account.service", defaultValue = "http://localhost:8080")
  String accountServiceUrl;

  @POST
  @Path("/{acctNumber}")
  public Response newTransaction(@PathParam("acctNumber") Long accountNumber, BigDecimal amount) {
    accountService.transact(accountNumber, amount);
    return Response.ok().build();
  }

  @POST
  @Path("/async/{acctNumber}")
  public CompletionStage<Void> newTransactionAsync(@PathParam("acctNumber") Long accountNumber, BigDecimal amount) {
    return accountService.transactAsync(accountNumber, amount);
  }

  @POST
  @Path("/api/{acctNumber}")
  public Response newTransactionWithApi(@PathParam("acctNumber") Long accountNumber, BigDecimal amount) throws MalformedURLException {
    AccountServiceProgrammatic acctService =
        RestClientBuilder.newBuilder()
            .baseUrl(new URL(accountServiceUrl))
            .build(AccountServiceProgrammatic.class);

    acctService.transact(accountNumber, amount);
    return Response.ok().build();
  }

  @POST
  @Path("/api/async/{acctNumber}")
  public CompletionStage<Void> newTransactionWithApiAsync(@PathParam("acctNumber") Long accountNumber, BigDecimal amount) throws MalformedURLException {
    AccountServiceProgrammatic acctService =
        RestClientBuilder.newBuilder()
            .baseUrl(new URL(accountServiceUrl))
            .build(AccountServiceProgrammatic.class);

    return acctService.transactAsync(accountNumber, amount);
  }
}
