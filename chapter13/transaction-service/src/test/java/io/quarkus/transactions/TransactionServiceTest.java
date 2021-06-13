package io.quarkus.transactions;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(WiremockAccountService.class)
public class TransactionServiceTest {
  @Test
  void testTransaction() {
        given()
            .body("142.12")
            .contentType(ContentType.JSON)
            .when().post("/transactions/{accountNumber}", 121212)
            .then()
            .statusCode(200);
  }
}
