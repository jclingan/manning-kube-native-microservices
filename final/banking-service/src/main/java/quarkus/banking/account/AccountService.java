package quarkus.banking.account;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RegisterRestClient(configKey = "account-api")
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AccountService {
  @PUT
  @Path("{accountNumber}/withdraw")
  Response withdrawal(@PathParam("accountNumber") Long accountNumber, String amount);

  @PUT
  @Path("{accountNumber}/deposit")
  Response deposit(@PathParam("accountNumber") Long accountNumber, String amount);
}
