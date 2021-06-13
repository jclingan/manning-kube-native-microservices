package io.quarkus.transactions;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Path("/accounts")
@RegisterRestClient
@ClientHeaderParam(name = "class-level-param", value = "AccountService-interface")
@RegisterClientHeaders
@RegisterProvider(AccountRequestFilter.class)
@RegisterProvider(AccountExceptionMapper.class)
@Produces(MediaType.APPLICATION_JSON)
public interface AccountService {
  @GET
  @Path("/jwt-secure/{acctNumber}/balance")
  BigDecimal getBalanceSecure(@PathParam("acctNumber") Long accountNumber);

  @GET
  @Path("/{acctNumber}/balance")
  BigDecimal getBalance(@PathParam("acctNumber") Long accountNumber);

  @POST
  @Path("{accountNumber}/transaction")
  Map<String, List<String>> transact(@PathParam("accountNumber") Long accountNumber, BigDecimal amount)
      throws AccountNotFoundException;

  @POST
  @Path("{accountNumber}/transaction")
  @ClientHeaderParam(name = "method-level-param", value = "{generateValue}")
  CompletionStage<Map<String, List<String>>> transactAsync(@PathParam("accountNumber") Long accountNumber,
      BigDecimal amount);

  default String generateValue() {
    return "Value generated in method for async call";
  }
}
