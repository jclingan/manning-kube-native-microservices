package io.quarkus.transactions;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

@Path("/accounts")
@RegisterRestClient
@Produces(MediaType.APPLICATION_JSON)
public interface AccountService {
  @GET
  @Path("/{acctNumber}/balance")
  BigDecimal getBalance(@PathParam("acctNumber") Long accountNumber);

  @POST
  @Path("{accountNumber}/transaction")
  void transact(@PathParam("accountNumber") Long accountNumber, BigDecimal amount);

  @POST
  @Path("{accountNumber}/transaction")
  CompletionStage<Void> transactAsync(@PathParam("accountNumber") Long accountNumber, BigDecimal amount);
}
