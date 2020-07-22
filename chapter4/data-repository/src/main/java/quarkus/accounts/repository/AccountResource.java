package quarkus.accounts.repository;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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

  @Inject
  AccountRepository accountRepository;

  @GET
  public List<Account> allAccounts() {
    return accountRepository.listAll();
  }

  @GET
  @Path("/{acctNumber}")
  public Account getAccount(@PathParam("acctNumber") Long accountNumber) {
    Account account = accountRepository.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    return account;
  }

  @POST
  @RolesAllowed({"admin"})
  @Transactional
  public Response createAccount(Account account) {
    if (account.getId() != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }

    // Ensure customer has not gone over max account limit
//    if (Account.totalAccountsForCustomer(account.customerNumber) >= customerAccountMaximum) {
//      throw new WebApplicationException("Customer already has maximum number of accounts: " + customerAccountMaximum, 409);
//    }

    accountRepository.persist(account);
    return Response.status(201).entity(account).build();
  }

  @PUT
  @Path("{accountNumber}/withdraw")
  @Transactional
  public Account withdrawal(@Context SecurityContext ctx, @PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity = accountRepository.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    if (entity.getAccountStatus().equals(AccountStatus.OVERDRAWN)) {
      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
    }

    entity.withdrawFunds(new BigDecimal(amount));

//    if (entity.balance.compareTo(accountOverdraftLimit) <= 0) {
//      entity.markOverdrawn();
//      accountEmitter.send(entity);
//    }

    return entity;
  }

  @PUT
  @Path("{accountNumber}/deposit")
  @Transactional
  public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity = accountRepository.findByAccountNumber(accountNumber);

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
    Account account = accountRepository.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    account.close();
    accountRepository.persist(account);
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
