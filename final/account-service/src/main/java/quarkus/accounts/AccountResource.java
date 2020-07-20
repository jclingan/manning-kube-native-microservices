package quarkus.accounts;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.util.List;

@Path("/accounts")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

  @ConfigProperty(name = "account.overdraft.limit", defaultValue = "0.00")
  BigDecimal accountOverdraftLimit;

  @ConfigProperty(name = "customer.account.max", defaultValue = "1")
  Integer customerAccountMaximum;

  @Channel("overdraft-exceeded")
  @OnOverflow(OnOverflow.Strategy.DROP)
  Emitter<Account> accountEmitter;

  @GET
  public List<Account> allAccounts() {
    return Account.listAll();
  }

  @GET
  @Path("/{acctNumber}")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully retrieved account",
          content = {
              @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = Account.class)
              )
          }),
      @APIResponse(responseCode = "404", description = "Account not found")
  })
  public Account getAccount(@PathParam("acctNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    return account;
  }

  @POST
  @RolesAllowed({"admin"})
  @Transactional
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully created a new account",
          content = {
          @Content(mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(implementation = Account.class)
          )
      }),
      @APIResponse(responseCode = "400", description = "Id was set on the Account object"),
      @APIResponse(responseCode = "409", description = "Customer is not able to open more accounts")
  })
  public Response createAccount(Account account) {
    if (account.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }

    // Ensure customer has not gone over max account limit
    if (Account.totalAccountsForCustomer(account.customerNumber) >= customerAccountMaximum) {
      throw new WebApplicationException("Customer already has maximum number of accounts: " + customerAccountMaximum, 409);
    }

    account.persist();
    return Response.status(201).entity(account).build();
  }

  @PUT
  @Path("{accountNumber}/withdraw")
  @RolesAllowed({"customer", "admin"})
  @Transactional
  public Account withdrawal(@Context SecurityContext ctx, @PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity = Account.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    if (ctx.isUserInRole("customer")) {
      if (!entity.customerName.equals(ctx.getUserPrincipal().getName())) {
        throw new WebApplicationException(ctx.getUserPrincipal().getName() + " is not authorized to withdraw from Account " + accountNumber, 403);
      }
    }

    if (entity.accountStatus.equals(AccountStatus.OVERDRAWN)) {
      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
    }

    entity.withdrawFunds(new BigDecimal(amount));

    if (entity.balance.compareTo(accountOverdraftLimit) <= 0) {
      entity.markOverdrawn();
      accountEmitter.send(entity);
    }

    return entity;
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
  @RolesAllowed({"admin"})
  @Transactional
  public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
    Account account = Account.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    account.close();
    account.persist();
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
