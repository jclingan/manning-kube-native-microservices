package quarkus.accounts;

import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import quarkus.accounts.events.OverdraftLimitUpdate;
import quarkus.accounts.events.Overdrawn;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

  @GET
  public List<Account> allAccounts() {
    return Account.listAll();
  }

  @GET
  @Path("/{acctNumber}")
  public Account getAccount(@PathParam("acctNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    return account;
  }

  @POST
  @Transactional
  public Response createAccount(Account account) {
    if (account.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }

    account.persist();
    return Response.status(201).entity(account).build();
  }

  @Inject
  @Channel("account-overdrawn")
  Emitter<Overdrawn> emitter;

  int ackedMessages = 0;
  List<Throwable> failures = new ArrayList<>();

  @PUT
  @Path("{accountNumber}/withdrawal")
  @Transactional
  public CompletionStage<Account> withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity = Account.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    if (entity.accountStatus.equals(AccountStatus.OVERDRAWN) && entity.balance.compareTo(entity.overdraftLimit) <= 0) {
      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
    }

    entity.withdrawFunds(new BigDecimal(amount));

    if (entity.balance.compareTo(BigDecimal.ZERO) < 0) {
      entity.markOverdrawn();
      entity.persist();
      Overdrawn payload = new Overdrawn(entity.accountNumber, entity.customerNumber, entity.balance, entity.overdraftLimit);
      return emitter.send(payload).thenCompose(empty -> CompletableFuture.completedFuture(entity));

      /* Alternative using ack and nack handlers
      CompletableFuture<Account> future = new CompletableFuture<>();
      emitter.send(Message.of(payload,
          () -> {
            ackedMessages++;
            future.complete(entity);
            return CompletableFuture.completedFuture(null);
          },
          reason -> {
            failures.add(reason);
            future.completeExceptionally(reason);
            return CompletableFuture.completedFuture(null);
          })
      );
      return future;
       */
    }

    entity.persist();
    return CompletableFuture.completedFuture(entity);
  }

  @Incoming("overdraft-update")
  @Blocking
  @Transactional
  public void processOverdraftUpdate(OverdraftLimitUpdate overdraftLimitUpdate) {
    Account account = Account.findByAccountNumber(overdraftLimitUpdate.accountNumber);
    account.overdraftLimit = overdraftLimitUpdate.newOverdraftLimit;
  }

  @PUT
  @Path("{accountNumber}/deposit")
  @Transactional
  public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity = Account.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    entity.addFunds(new BigDecimal(amount));
    return entity;
  }

  @DELETE
  @Path("{accountNumber}")
  @Transactional
  public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    account.close();
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
