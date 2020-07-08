package quarkus.accounts;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Path("/accounts")
@ApplicationScoped
public class AccountResource {

  Set<Account> accounts = new HashSet<>();

  @PostConstruct
  public void setup() {
    accounts.add(new Account(123456789L, 987654321L, "George Baird", new BigDecimal("354.23")));
    accounts.add(new Account(121212121L, 888777666L, "Mary Taylor", new BigDecimal("560.03")));
    accounts.add(new Account(545454545L, 222444999L, "Diana Rigg", new BigDecimal("422.00")));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Account> allAccounts() {
    return accounts;
  }

  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Account getAccount(@PathParam("id") Long accountId) {
    Account response = null;
    for (Account acct : accounts) {
      if (acct.getAccountNumber().equals(accountId)) {
        response = acct;
        break;
      }
    }

    if (response == null) {
      throw new WebApplicationException("Account with id of " + accountId + " does not exist.", 404);
    }

    return response;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createAccount(Account account) {
    if (account.getAccountNumber() == null) {
      throw new WebApplicationException("No Account number specified.", 400);
    }

    accounts.add(account);
    return Response.status(201).entity(account).build();
  }

  @PUT
  @Path("{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Account updateAccount(@PathParam("id") Long accountId, Account account) {
    Account oldAccount = getAccount(accountId);
    accounts.remove(oldAccount);
    accounts.add(account);
    return account;
  }

  @DELETE
  @Path("{id}")
  public Response deleteAccount(@PathParam("id") Long accountId) {
    Account oldAccount = getAccount(accountId);
    accounts.remove(oldAccount);
    return Response.noContent().build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

      int code = 500;
      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
          .add("exceptionType", exception.getClass().getName())
          .add("code", code);

      if (exception.getMessage() != null) {
        entityBuilder.add("error", exception.getMessage());
      }

      return Response.status(code)
          .entity(entityBuilder.build())
          .build();
    }
  }
}
