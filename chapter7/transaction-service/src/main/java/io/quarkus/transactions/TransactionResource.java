package io.quarkus.transactions;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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
  public Map<String, List<String>> newTransaction(@PathParam("acctNumber") Long accountNumber, BigDecimal amount) {
    try {
      return accountService.transact(accountNumber, amount);
    } catch (Throwable t) {
      t.printStackTrace();
      Map<String, List<String>> response = new HashMap<>();
      response.put("EXCEPTION - " + t.getClass(), Collections.singletonList(t.getMessage()));
      return response;
    }
  }

  @POST
  @Path("/async/{acctNumber}")
  public CompletionStage<Map<String, List<String>>> newTransactionAsync(@PathParam("acctNumber") Long accountNumber,
      BigDecimal amount) {
    return accountService.transactAsync(accountNumber, amount);
  }

  @POST
  @Path("/api/{acctNumber}")
  @Bulkhead(1)
  @CircuitBreaker(
    requestVolumeThreshold=3,
    failureRatio=.66,
    delay = 1,
    delayUnit = ChronoUnit.SECONDS,
    successThreshold=2
  )
  @Fallback(value = TransactionServiceFallbackHandler.class)
  public Response newTransactionWithApi(@PathParam("acctNumber") Long accountNumber, BigDecimal amount)
      throws MalformedURLException {
    AccountServiceProgrammatic acctService = RestClientBuilder.newBuilder().baseUrl(new URL(accountServiceUrl))
        .connectTimeout(500, TimeUnit.MILLISECONDS).readTimeout(1200, TimeUnit.MILLISECONDS)
        .build(AccountServiceProgrammatic.class);

    acctService.transact(accountNumber, amount);
    return Response.ok().build();
  }

  public Response bulkheadFallbackGetBalance(Long accountNumber, BigDecimal amount) {
    return Response.status(Response.Status.TOO_MANY_REQUESTS).build();
  }

  @POST
  @Path("/api/async/{acctNumber}")
  public CompletionStage<Void> newTransactionWithApiAsync(@PathParam("acctNumber") Long accountNumber,
      BigDecimal amount) throws MalformedURLException {
    AccountServiceProgrammatic acctService = RestClientBuilder.newBuilder().baseUrl(new URL(accountServiceUrl))
        .build(AccountServiceProgrammatic.class);

    return acctService.transactAsync(accountNumber, amount);
  }

  @GET
  @Path("/{acctnumber}/balance")
  @Timeout(100)
  @Retry(delay = 100,
         jitter = 25,
         maxRetries = 3,
         retryOn = TimeoutException.class)
  @Fallback(value = TransactionServiceFallbackHandler.class)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBalance( // <3>
      @PathParam("acctnumber") Long accountNumber) {
    String balance = accountService.getBalance(accountNumber).toString();

    return Response.ok(balance).build();
  }

  public Response timeoutFallbackGetBalance(Long accountNumber) {
    return Response.status(Response.Status.GATEWAY_TIMEOUT).build(); // <4>
  }
}
