package quarkus.accounts.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
  EntityManager entityManager;

  @GET
  public List<Account> allAccounts() {
    return entityManager.createNamedQuery("Accounts.findAll", Account.class).getResultList();
  }

  @GET
  @Path("/{acctNumber}")
  public Account getAccount(@PathParam("acctNumber") Long accountNumber) {
    try {
      return entityManager.createNamedQuery("Accounts.findByAccountNumber", Account.class).setParameter("accountNumber", accountNumber).getSingleResult();
    } catch (NoResultException nre) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }
  }

  @POST
  @Transactional
  public Response createAccount(Account account) {
    if (account.getId() != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }

    entityManager.persist(account);
    return Response.status(201).entity(account).build();
  }

  @PUT
  @Path("{accountNumber}/withdrawal")
  @Transactional
  public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity;
    try {
      entity = getAccount(accountNumber);
    } catch (NoResultException nre) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    if (entity.getAccountStatus().equals(AccountStatus.OVERDRAWN)) {
      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
    }

    entity.withdrawFunds(new BigDecimal(amount));
    return entity;
  }

  @PUT
  @Path("{accountNumber}/deposit")
  @Transactional
  public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account entity;
    try {
      entity = getAccount(accountNumber);
    } catch (NoResultException nre) {
      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
    }

    entity.addFunds(new BigDecimal(amount));
    return entity;
  }

  @DELETE
  @Path("{accountNumber}")
  @Transactional
  public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
    Account account;
    try {
      account = getAccount(accountNumber);
    } catch (NoResultException nre) {
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
