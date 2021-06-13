package quarkus.accounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Path("/accounts")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {
  @RolesAllowed("customer")
  @GET
  @Path("/jwt-secure/{acctNumber}/balance")
  public BigDecimal getBalanceJWT(@PathParam("acctNumber") Long accountNumber) {
    return getBalance(accountNumber);
  }

  @GET
  @Path("/{acctNumber}/balance")
  public BigDecimal getBalance(@PathParam("acctNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    return account.balance;
  }

  @POST
  @Path("{accountNumber}/transaction")
  @Transactional
  public Map<String, List<String>> transact(@Context HttpHeaders headers,
      @PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
    Account entity = Account.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    if (entity.accountStatus.equals(AccountStatus.OVERDRAWN)) {
      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
    }

    entity.balance = entity.balance.add(amount);
    return headers.getRequestHeaders();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {
    @Metric(name = "ErrorMapperCounter", description = "Number of times the AccountResource ErrorMapper is invoked")
    Counter errorMapperCounter;

    @Override
    public Response toResponse(Exception exception) {
      int code = 500;

      errorMapperCounter.inc();

      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      JsonObjectBuilder entityBuilder = Json.createObjectBuilder().add("exceptionType", exception.getClass().getName())
          .add("code", code);

      if (exception.getMessage() != null) {
        entityBuilder.add("error", exception.getMessage());
      }

      return Response.status(code).entity(entityBuilder.build()).build();
    }
  }
}
