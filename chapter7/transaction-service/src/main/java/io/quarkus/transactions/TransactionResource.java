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
  @Bulkhead(1) // <1>
  @CircuitBreaker(
    requestVolumeThreshold=3,                                           // <1>
    failureRatio=.66,                                                   // <2>
    delay = 5,                                                          // <3>
    delayUnit = ChronoUnit.SECONDS,                                     // <4>
    successThreshold=2                                                  // <5>
  )
  @Fallback(value = TransactionFallbackHandler.class)
  @Path("/api/{acctNumber}")
  public Response newTransactionWithApi(@PathParam("acctNumber") Long accountNumber, BigDecimal amount)
      throws MalformedURLException {
    AccountServiceProgrammatic acctService = RestClientBuilder.newBuilder().baseUrl(new URL(accountServiceUrl))
        .connectTimeout(500, TimeUnit.MILLISECONDS).readTimeout(1200, TimeUnit.MILLISECONDS)
        .build(AccountServiceProgrammatic.class);

    acctService.transact(accountNumber, amount);
    return Response.ok().build();
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
  @Path("/timeout/{acctnumber}/balance")
  @Timeout(100) // <1>
  @Retry(delay = 100,                                                // (1)
         jitter = 25,                                              // (2)
         maxRetries = 3,                                           // (3)
         retryOn = TimeoutException.class)                         
  @Fallback(value = TransactionFallbackHandler.class)                      // <7>

  @Produces(MediaType.APPLICATION_JSON)
  public Response getBalance( // <3>
      @PathParam("acctnumber") Long accountNumber) {
    String balance = accountService.getBalance(accountNumber).toString();

    return Response.ok(balance).build();
  }

  public Response bulkheadFallbackGetBalance(Long accountNumber, BigDecimal amount) { // <3>
    return Response.status(Response.Status.TOO_MANY_REQUESTS).build(); // <4>
  }

  public Response timeoutFallbackGetBalance(Long accountNumber) {
    return Response.status(Response.Status.GATEWAY_TIMEOUT).build(); // <4>
  }
}
