package quarkus.accounts;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySink;
import io.smallrye.reactive.messaging.connectors.InMemorySource;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.*;
import quarkus.accounts.events.OverdraftLimitUpdate;
import quarkus.accounts.events.Overdrawn;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@QuarkusTestResource(InMemoryLifecycleManager.class)
public class AccountResourceEventsTest {

  @Inject
  @Any
  InMemoryConnector connector;

  @Test
  void testOverdraftEvent() {
    InMemorySink<Overdrawn> overdrawnSink = connector.sink("account-overdrawn");

    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 78790)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    BigDecimal withdrawal = new BigDecimal("23.82");
    BigDecimal balance = account.balance.subtract(withdrawal);

    account =
        given()
            .contentType(ContentType.JSON)
            .body(withdrawal.toString())
            .when().put("/accounts/{accountNumber}/withdrawal", 78790)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.accountNumber, equalTo(78790L));
    assertThat(account.customerName, equalTo("Vanna White"));
    assertThat(account.customerNumber, equalTo(444222L));
    assertThat(account.accountStatus, equalTo(AccountStatus.OPEN));
    assertThat(account.balance, equalTo(balance));

    assertThat(overdrawnSink.received().size(), equalTo(0));

    withdrawal = new BigDecimal("6000.00");
    balance = account.balance.subtract(withdrawal);

    account =
        given()
            .contentType(ContentType.JSON)
            .body(withdrawal.toString())
            .when().put("/accounts/{accountNumber}/withdrawal", 78790)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.accountNumber, equalTo(78790L));
    assertThat(account.customerName, equalTo("Vanna White"));
    assertThat(account.customerNumber, equalTo(444222L));
    assertThat(account.accountStatus, equalTo(AccountStatus.OVERDRAWN));
    assertThat(account.balance, equalTo(balance));

    assertThat(overdrawnSink.received().size(), equalTo(1));

    Message<Overdrawn> overdrawnMsg = overdrawnSink.received().get(0);
    assertThat(overdrawnMsg, notNullValue());
    Overdrawn event = overdrawnMsg.getPayload();
    assertThat(event.accountNumber, equalTo(78790L));
    assertThat(event.customerNumber, equalTo(444222L));
    assertThat(event.balance, equalTo(balance));
    assertThat(event.overdraftLimit, equalTo(new BigDecimal("-200.00")));
  }

  @Test
  void testOverdraftUpdate() {
    InMemorySource<OverdraftLimitUpdate> source = connector.source("overdraft-update");

    Account account =
        given()
            .when().get("/accounts/{accountNumber}", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.accountNumber, equalTo(123456789L));
    assertThat(account.customerName, equalTo("Debbie Hall"));
    assertThat(account.customerNumber, equalTo(12345L));
    assertThat(account.accountStatus, equalTo(AccountStatus.OPEN));
    assertThat(account.overdraftLimit, equalTo(new BigDecimal("-200.00")));

    OverdraftLimitUpdate updateEvent = new OverdraftLimitUpdate();
    updateEvent.accountNumber = 123456789L;
    updateEvent.newOverdraftLimit = new BigDecimal("-600.00");

    source.send(updateEvent);

    account =
        given()
            .when().get("/accounts/{accountNumber}", 123456789)
            .then()
            .statusCode(200)
            .extract()
            .as(Account.class);

    assertThat(account.accountNumber, equalTo(123456789L));
    assertThat(account.customerName, equalTo("Debbie Hall"));
    assertThat(account.customerNumber, equalTo(12345L));
    assertThat(account.accountStatus, equalTo(AccountStatus.OPEN));
    assertThat(account.overdraftLimit, equalTo(new BigDecimal("-600.00")));
  }
}
