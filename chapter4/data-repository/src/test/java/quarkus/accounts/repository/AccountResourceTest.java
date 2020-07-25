package quarkus.accounts.repository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.List;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestMethodOrder(OrderAnnotation.class)
public class AccountResourceTest {
  @Test
  @Order(1)
  void testRetrieveAll() {
    Response result =
        given()
          .when().get("/accounts")
          .then()
            .statusCode(200)
            .body(
                containsString("Debbie Hall"),
                containsString("David Tennant"),
                containsString("Alex Kingston")
            )
            .extract()
            .response();

    List<Account> accounts = result.jsonPath().getList("$");
    assertThat(accounts, not(empty()));
    assertThat(accounts, hasSize(8));
  }

  @Test
  @Order(2)
  void testGetAccount() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 444666)
            .then()
              .statusCode(200)
              .extract()
              .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(444666L));
    assertThat(account.getCustomerName(), equalTo("Billie Piper"));
    assertThat(account.getCustomerNumber(), equalTo(332233L));
    assertThat(account.getBalance(), equalTo(new BigDecimal("3499.12")));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));
  }

  @Test
  @Order(3)
  void testCreateAccount() throws Exception {
    Account newAccount = new Account();
    newAccount.setAccountNumber(324324L);
    newAccount.setCustomerNumber(112244L);
    newAccount.setCustomerName("Sandy Holmes");
    newAccount.setBalance(new BigDecimal("154.55"));

    Account returnedAccount =
        given()
          .contentType(ContentType.JSON)
          .body(newAccount)
          .when().post("/accounts")
          .then()
            .statusCode(201)
            .extract()
            .as(Account.class);

    assertThat(returnedAccount, notNullValue());
    assertThat(returnedAccount, equalTo(newAccount));

    Response result =
        given()
            .when().get("/accounts")
            .then()
            .statusCode(200)
            .body(
                containsString("Debbie Hall"),
                containsString("David Tennant"),
                containsString("Alex Kingston"),
                containsString("Sandy Holmes")
            )
            .extract()
            .response();

    List<Account> accounts = result.jsonPath().getList("$");
    assertThat(accounts, not(empty()));
    assertThat(accounts, hasSize(9));
  }

  @Test
  @Order(4)
  void testCloseAccount() {
    given()
        .when().delete("/accounts/{accountNumber}", 5465)
        .then()
        .statusCode(204);

    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 5465)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(5465L));
    assertThat(account.getCustomerName(), equalTo("Alex Trebek"));
    assertThat(account.getCustomerNumber(), equalTo(776868L));
    assertThat(account.getBalance(), equalTo(new BigDecimal("0.00")));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.CLOSED));
  }

  @Test
  @Order(5)
  void testDeposit() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    BigDecimal deposit = new BigDecimal("154.98");
    BigDecimal balance = account.getBalance().add(deposit);

    account =
        given()
            .contentType(ContentType.JSON)
            .body(deposit.toString())
            .when().put("/accounts/{accountNumber}/deposit", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(123456789L));
    assertThat(account.getCustomerName(), equalTo("Debbie Hall"));
    assertThat(account.getCustomerNumber(), equalTo(12345L));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));
    assertThat(account.getBalance(), equalTo(balance));
  }

  @Test
  @Order(6)
  void testWithdrawal() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 78790)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    BigDecimal withdrawal = new BigDecimal("23.82");
    BigDecimal balance = account.getBalance().subtract(withdrawal);

    account =
        given()
            .contentType(ContentType.JSON)
            .body(withdrawal.toString())
            .when().put("/accounts/{accountNumber}/withdrawal", 78790)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(78790L));
    assertThat(account.getCustomerName(), equalTo("Vanna White"));
    assertThat(account.getCustomerNumber(), equalTo(444222L));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));
    assertThat(account.getBalance(), equalTo(balance));
  }
}
