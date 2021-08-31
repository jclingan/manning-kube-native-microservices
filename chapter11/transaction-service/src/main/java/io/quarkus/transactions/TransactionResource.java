package io.quarkus.transactions;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {
  @Inject
  @RestClient
  AccountService accountService;

  @PUT
  @Path("/{acctNumber}/withdrawal")
  public Account withdrawal(@PathParam("acctNumber") Long accountNumber, String amount) throws AccountNotFoundException {
      return accountService.withdrawal(accountNumber, amount);
  }

  @PUT
  @Path("/{acctNumber}/deposit")
  public Response deposit(@PathParam("acctNumber") Long accountNumber, String amount) {
    try {
      accountService.deposit(accountNumber, amount);
      return Response.ok().build();
    } catch (Throwable t) {
      return Response.serverError().entity(t).build();
    }
  }
}
