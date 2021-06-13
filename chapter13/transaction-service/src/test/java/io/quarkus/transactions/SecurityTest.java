package io.quarkus.transactions;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@QuarkusTestResource(WiremockAccountService.class)
@TestSecurity(user = "duke", roles = { "customer" })
public class SecurityTest {
    @Test
    public void built_in_security() {
        given()
          .when()
          .get("/transactions/config-secure/{acctNumber}/balance", 121212)
        .then()
          .statusCode(200)
          .body(CoreMatchers.containsString("435.76"));
    }
}