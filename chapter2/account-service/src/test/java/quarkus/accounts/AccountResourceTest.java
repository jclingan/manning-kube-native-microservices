package quarkus.accounts;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

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
                containsString("George Baird"),
                containsString("Mary Taylor"),
                containsString("Diana Rigg")
            )
            .extract()
            .response();

    List<Account> accounts = result.jsonPath().getList("$");
    assertThat(accounts, not(empty()));
    assertThat(accounts, hasSize(3));
  }

  @Test
  @Order(2)
  void testGetAccount() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 545454545)
            .then()
              .statusCode(200)
              .extract()
              .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(545454545L));
    assertThat(account.getCustomerName(), equalTo("Diana Rigg"));
    assertThat(account.getBalance(), equalTo(new BigDecimal("422.00")));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));
  }

  @Test
  @Order(3)
  void testCreateAccount() {
    Account newAccount = new Account(324324L, 112244L, "Sandy Holmes", new BigDecimal("154.55"));

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
                containsString("George Baird"),
                containsString("Mary Taylor"),
                containsString("Diana Rigg"),
                containsString("Sandy Holmes")
            )
            .extract()
            .response();

    List<Account> accounts = result.jsonPath().getList("$");
    assertThat(accounts, not(empty()));
    assertThat(accounts, hasSize(4));
  }

  @Test
  @Order(4)
  void testAccountWithdraw() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 545454545)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(545454545L));
    assertThat(account.getCustomerName(), equalTo("Diana Rigg"));
    assertThat(account.getBalance(), equalTo(new BigDecimal("422.00")));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));

    Account result =
        given()
            .body("56.21")
            .when().put("/accounts/{accountNumber}/withdrawal", 545454545)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(result.getAccountNumber(), equalTo(545454545L));
    assertThat(result.getCustomerName(), equalTo("Diana Rigg"));
    assertThat(result.getBalance(), equalTo(account.getBalance().subtract(new BigDecimal("56.21"))));
    assertThat(result.getAccountStatus(), equalTo(AccountStatus.OPEN));
  }

  @Test
  @Order(4)
  void testAccountDeposit() {
    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.getAccountNumber(), equalTo(123456789L));
    assertThat(account.getCustomerName(), equalTo("George Baird"));
    assertThat(account.getBalance(), equalTo(new BigDecimal("354.23")));
    assertThat(account.getAccountStatus(), equalTo(AccountStatus.OPEN));

    Account result =
        given()
            .body("28.42")
            .when().put("/accounts/{accountNumber}/deposit", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(result.getAccountNumber(), equalTo(123456789L));
    assertThat(result.getCustomerName(), equalTo("George Baird"));
    assertThat(result.getBalance(), equalTo(account.getBalance().add(new BigDecimal("28.42"))));
    assertThat(result.getAccountStatus(), equalTo(AccountStatus.OPEN));
  }
}
