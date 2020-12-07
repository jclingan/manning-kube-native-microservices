package io.quarkus.transactions;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/accounts")
@RegisterRestClient
@Produces(MediaType.APPLICATION_JSON)
public interface AccountService {
  @PUT
  @Path("{accountNumber}/withdrawal")
  Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) throws AccountNotFoundException;

  @PUT
  @Path("{accountNumber}/deposit")
  Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) throws AccountNotFoundException;
}
