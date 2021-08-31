package quarkus.accounts;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.annotation.PostConstruct;
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
@Tag(name = "transactions",
    description = "Operations manipulating account balances.")
@Tag(name = "admin",
    description = "Operations for managing accounts.")
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
  @APIResponse(responseCode = "200", description = "Retrieved all Accounts",
      content = @Content(
          schema = @Schema(
              type = SchemaType.ARRAY,
              implementation = Account.class)
      )
  )
  @Tag(name = "admin")
  public Set<Account> allAccounts() {
    return accounts;
  }

  @GET
  @Path("/{accountNumber}")
  @Produces(MediaType.APPLICATION_JSON)
  @APIResponse(responseCode = "200", description = "Successfully retrieved an account.",
      content = @Content(
          schema = @Schema(implementation = Account.class))
  )
  @APIResponse(responseCode = "400", description = "Account with id of {accountNumber} does not exist.",
      content = @Content(
          schema = @Schema(
              implementation = ErrorResponse.class,
              example = "{\n" +
                  "\"exceptionType\": \"javax.ws.rs.WebApplicationException\",\n" +
                  "\"code\": 400,\n" +
                  "\"error\": \"Account with id of 12345678 does not exist.\"\n" +
                  "}\n")
      )
  )
  @Tag(name = "admin")
  public Account getAccount(
      @Parameter(
          name = "accountNumber",
          description = "Number of the Account instance to be retrieved.",
          required = true,
          in = ParameterIn.PATH
      )
      @PathParam("accountNumber") Long accountNumber) {
    Account response = null;
    for (Account acct : accounts) {
      if (acct.getAccountNumber().equals(accountNumber)) {
        response = acct;
        break;
      }
    }

    if (response == null) {
      throw new WebApplicationException("Account with id of " + accountNumber + " does not exist.", 404);
    }

    return response;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Create a new bank account.")
  @APIResponse(responseCode = "201", description = "Successfully created a new account.",
      content = @Content(
          schema = @Schema(implementation = Account.class))
  )
  @APIResponse(responseCode = "400", description = "No account number was specified on the Account.",
      content = @Content(
          schema = @Schema(
              implementation = ErrorResponse.class,
              example = "{\n" +
                  "\"exceptionType\": \"javax.ws.rs.WebApplicationException\",\n" +
                  "\"code\": 400,\n" +
                  "\"error\": \"No Account number specified.\"\n" +
                  "}\n")
      )
  )
  @Tag(name = "admin")
  public Response createAccount(Account account) {
    if (account.getAccountNumber() == null) {
      throw new WebApplicationException("No Account number specified.", 400);
    }

    accounts.add(account);
    return Response.status(201).entity(account).build();
  }

  @PUT
  @Path("{accountNumber}/withdrawal")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Tag(name = "transactions")
  public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
    Account account = getAccount(accountNumber);
    account.withdrawFunds(new BigDecimal(amount));
    return account;
  }

  @PUT
  @Path("{accountNumber}/deposit")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @APIResponse(responseCode = "200", description = "Successfully deposited funds to an account.",
      content = @Content(
          schema = @Schema(implementation = Account.class))
  )
  @RequestBody(
      name = "amount",
      description = "Amount to be deposited into the account.",
      required = true,
      content = @Content(
          schema = @Schema(
              name = "amount",
              type = SchemaType.STRING,
              required = true,
              minLength = 4),
          example = "435.61"
      )
  )
  @Tag(name = "transactions")
  public Account deposit(
      @Parameter(
          name = "accountNumber",
          description = "Number of the Account to deposit into.",
          required = true,
          in = ParameterIn.PATH
      )
      @PathParam("accountNumber") Long accountNumber,
      String amount) {
    Account account = getAccount(accountNumber);
    account.addFunds(new BigDecimal(amount));
    return account;
  }

  @DELETE
  @Path("{accountNumber}")
  @Tag(name = "admin")
  public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
    Account oldAccount = getAccount(accountNumber);
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

  private static class ErrorResponse {
    @Schema(required = true, example = "javax.ws.rs.WebApplicationException")
    public String exceptionType;
    @Schema(required = true, example = "400", type = SchemaType.INTEGER)
    public Integer code;
    public String error;
  }
}
