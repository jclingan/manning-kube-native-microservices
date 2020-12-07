package quarkus.accounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/accounts")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

  @ConfigProperty(name = "ft.delay", defaultValue = "200") // <1>
  int delay;

  void doDelay() {
    int delayTime;
    try {
      delayTime = new Random().nextInt(delay); // <2>
      System.out.println("** Waiting " + delayTime + "ms **");
      TimeUnit.MILLISECONDS.sleep(delayTime); // <3>
    } catch (InterruptedException e) {
      // e.printStackTrace();
    }
  }

  @GET
  @Path("/{acctNumber}/balance")
  public BigDecimal getBalance(@PathParam("acctNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    doDelay();                     // <1>

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

    doDelay();    // <1>

    entity.balance = entity.balance.add(amount);
    return headers.getRequestHeaders();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

      int code = 500;
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
