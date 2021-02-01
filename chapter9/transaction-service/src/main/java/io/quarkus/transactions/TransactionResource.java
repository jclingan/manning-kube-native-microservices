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

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/transactions",
                produces=MediaType.APPLICATION_JSON_VALUE,
                consumes=MediaType.APPLICATION_JSON_VALUE)
public class TransactionResource {
  @Autowired
  @RestClient
  AccountService accountService;

  @Value("${account.service:http://localhost:8080}")
  String accountServiceUrl;

  @PostMapping("/{acctNumber}")
  public Map<String, List<String>> newTransaction(@PathVariable("acctNumber") Long accountNumber, @RequestBody BigDecimal amount) {
    try {
      return accountService.transact(accountNumber, amount);
    } catch (Throwable t) {
      t.printStackTrace();
      Map<String, List<String>> response = new HashMap<String,List<String>>();
      response.put("EXCEPTION - " + t.getClass(), Collections.singletonList(t.getMessage()));
      return response;
    }
  }

  @PostMapping("/async/{acctNumber}")
  public CompletionStage<Map<String, List<String>>> newTransactionAsync(@PathVariable("acctNumber") Long accountNumber,
      @RequestBody BigDecimal amount) {
    return accountService.transactAsync(accountNumber, amount);
  }


  @PostMapping("/api/{acctNumber}")
  @Bulkhead(1)
  @CircuitBreaker(
    requestVolumeThreshold=3,
    failureRatio=.66,
    delay = 1,
    delayUnit = ChronoUnit.SECONDS,
    successThreshold=2
  )
  @Fallback(value = TransactionServiceFallbackHandler.class)
  public ResponseEntity<String> newTransactionWithApi(@PathVariable("acctNumber") Long accountNumber, @RequestBody BigDecimal amount)
      throws MalformedURLException {
    AccountServiceProgrammatic acctService = RestClientBuilder.newBuilder().baseUrl(new URL(accountServiceUrl))
        .connectTimeout(500, TimeUnit.MILLISECONDS).readTimeout(1200, TimeUnit.MILLISECONDS)
        .build(AccountServiceProgrammatic.class);

    acctService.transact(accountNumber, amount);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  public ResponseEntity<String> bulkheadFallbackGetBalance(Long accountNumber, BigDecimal amount) {
    return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
  }

  @PostMapping("/api/async/{accountNumber}")
  public CompletionStage<Void> newTransactionWithApiAsync(@PathVariable("acctNumber") Long accountNumber,
      @RequestBody BigDecimal amount) throws MalformedURLException {
    AccountServiceProgrammatic acctService = RestClientBuilder.newBuilder().baseUrl(new URL(accountServiceUrl))
        .build(AccountServiceProgrammatic.class);

    return acctService.transactAsync(accountNumber, amount);
  }

  @GetMapping("/{acctNumber}/balance")
  @Timeout(100)
  @Retry(delay = 100,
         jitter = 25,
         maxRetries = 3,
         retryOn = TimeoutException.class)
  @Fallback(value = TransactionServiceFallbackHandler.class)
  public ResponseEntity<String> getBalance( @PathVariable("acctNumber") Long accountNumber) {
    String balance = accountService.getBalance(accountNumber).toString();

    return new ResponseEntity<>(balance, HttpStatus.OK);
  }

  public ResponseEntity<String> timeoutFallbackGetBalance(Long accountNumber) {
    return new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
  }
}
