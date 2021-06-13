package io.quarkus.transactions;

import static io.restassured.RestAssured.given;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@QuarkusTestResource(WiremockAccountService.class)                    // <1>
public class FaultyAccountServiceTest {
  @Test
  void testTimeout() {
    given()
      .contentType(ContentType.JSON)
    .get("/transactions/123456/balance").then().statusCode(504);      // <2>

    given()
      .contentType(ContentType.JSON)
    .get("/transactions/456789/balance").then().statusCode(200);      // <3>
  }

  @Test
  void testCircuitBreaker() {
    RequestSpecification request =
      given()
        .body("142.12")
        .contentType(ContentType.JSON);

    request.post("/transactions/api/444666").then().statusCode(200);
    request.post("/transactions/api/444666").then().statusCode(502);
    request.post("/transactions/api/444666").then().statusCode(502);
    request.post("/transactions/api/444666").then().statusCode(503);
    request.post("/transactions/api/444666").then().statusCode(503);

    try {
      TimeUnit.MILLISECONDS.sleep(1000);
    } catch (InterruptedException e) {
    }

    request.post("/transactions/api/444666").then().statusCode(200);
    request.post("/transactions/api/444666").then().statusCode(200);
  }
}