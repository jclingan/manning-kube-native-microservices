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
}
