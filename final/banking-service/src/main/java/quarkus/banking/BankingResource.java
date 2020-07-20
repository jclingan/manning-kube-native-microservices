package quarkus.banking;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quarkus.banking.account.AccountService;

import javax.enterprise.context.ApplicationScoped;
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
import java.util.List;
import java.util.logging.Logger;

@Path("/banking")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankingResource {
  Logger LOGGER = org.jboss.logmanager.Logger.getLogger(BankingResource.class.getName());

  @ConfigProperty(name = "account.daily.transaction.max", defaultValue = "1500.00")
  BigDecimal maxDailyTransactionTotal;

  @Inject
  TransactionRecordRepository transactionRecordRepository;

  @Inject
  @RestClient
  AccountService accountService;

  @GET
  public List<TransactionRecord> allAccounts() {
    return transactionRecordRepository.listAll();
  }

  @GET
  @Path("{id")
  public TransactionRecord getTransaction(@PathParam("id") Long id) {
    TransactionRecord transactionRecord = transactionRecordRepository.findById(id);

    if (transactionRecord == null) {
      throw new WebApplicationException("Transaction with id of " + id + " does not exist.", 404);
    }

    return transactionRecord;
  }

  @POST
  @Transactional
  public Response createTransactionRecord(TransactionRecord transactionRecord) {
    if (transactionRecord.getId() != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 400);
    }

    boolean withdrawal = false;

    // Verify customer has not exceeded daily withdrawal limit
    if (transactionRecord.getAmount().compareTo(BigDecimal.ZERO) < 0) {
      withdrawal = true;
      BigDecimal currentTotal =
          transactionRecordRepository.totalDailyWithdrawals(transactionRecord.getAccountNumber());
      if (currentTotal.negate().add(transactionRecord.getAmount()).compareTo(maxDailyTransactionTotal) > 0) {
        throw new WebApplicationException("Customer would exceed daily withdrawal limit", 409);
      }
    }

    // Call Account service to deposit/withdraw
    Response externalResponse = null;
    if (withdrawal) {
      externalResponse = accountService.withdrawal(transactionRecord.getAccountNumber(), transactionRecord.getAmount().toString());
    } else {
      externalResponse = accountService.deposit(transactionRecord.getAccountNumber(), transactionRecord.getAmount().toString());
    }

    if (externalResponse.getStatus() != Response.Status.OK.getStatusCode()) {
      LOGGER.fine(externalResponse.getStatusInfo().getReasonPhrase());
      throw new WebApplicationException("Failed to update account balance, please try again later", 500);
    }

    transactionRecordRepository.persist(transactionRecord);
    return Response.status(201).entity(transactionRecord).build();
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
