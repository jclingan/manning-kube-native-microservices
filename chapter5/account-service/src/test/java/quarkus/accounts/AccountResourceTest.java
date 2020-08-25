package quarkus.accounts;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountResourceTest {

  @Test
  @Order(1)
  void testBalanceRetrieval() {
    BigDecimal balance =
        given()
            .when().get("/accounts/{accountNumber}/balance", 444666)
            .then()
            .statusCode(200)
            .extract()
            .as(BigDecimal.class);

    assertThat(balance, equalTo(new BigDecimal("3499.12")));
  }

  @Test
  @Order(2)
  void testWithdrawFunds() {
    BigDecimal balance =
        given()
            .when().get("/accounts/{accountNumber}/balance", 444666)
            .then()
            .statusCode(200)
            .extract()
            .as(BigDecimal.class);

    assertThat(balance, equalTo(new BigDecimal("3499.12")));

    BigDecimal withdrawalAmt = new BigDecimal("-345.15");
    BigDecimal expectedBalance = new BigDecimal("3153.97");

    given()
        .contentType(ContentType.JSON)
        .body(withdrawalAmt)
        .when().post("/accounts/{accountNumber}/transaction", 444666)
        .then()
        .statusCode(204);

    balance =
        given()
            .when().get("/accounts/{accountNumber}/balance", 444666)
            .then()
            .statusCode(200)
            .extract()
            .as(BigDecimal.class);

    assertThat(balance, equalTo(expectedBalance));
  }

  @Test
  @Order(3)
  void testDepositFunds() {
    BigDecimal balance =
        given()
            .when().get("/accounts/{accountNumber}/balance", 444666)
            .then()
            .statusCode(200)
            .extract()
            .as(BigDecimal.class);

    assertThat(balance, equalTo(new BigDecimal("3153.97")));

    BigDecimal depositAmt = new BigDecimal("139.43");
    BigDecimal expectedBalance = new BigDecimal("3293.40");

    given()
        .contentType(ContentType.JSON)
        .body(depositAmt)
        .when().post("/accounts/{accountNumber}/transaction", 444666)
        .then()
        .statusCode(204);

    balance =
        given()
            .when().get("/accounts/{accountNumber}/balance", 444666)
            .then()
            .statusCode(200)
            .extract()
            .as(BigDecimal.class);

    assertThat(balance, equalTo(expectedBalance));
  }
}
