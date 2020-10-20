package quarkus.overdraft;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySink;
import io.smallrye.reactive.messaging.connectors.InMemorySource;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import quarkus.overdraft.events.OverdraftFee;
import quarkus.overdraft.events.OverdraftLimitUpdate;
import quarkus.overdraft.events.Overdrawn;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@QuarkusTestResource(InMemoryLifecycleManager.class)
public class OverdraftResourceEventsTest {
  @Inject
  @Any
  InMemoryConnector connector;

  @Test
  void testOverdraftEvent() {
    InMemorySource<Overdrawn> overdrawnSource = connector.source("account-overdrawn");
    InMemorySink<OverdraftFee> overdraftSink = connector.sink("overdraft-fee");

    Overdrawn overdrawn = new Overdrawn(121212L, 212121L, new BigDecimal("-185.00"), new BigDecimal("-200.00"));
    overdrawnSource.send(overdrawn);

    await().atMost(3, TimeUnit.SECONDS).until(() -> overdraftSink.received().size() == 1);

    Message<OverdraftFee> overdraftFeeMessage = overdraftSink.received().get(0);
    assertThat(overdraftFeeMessage, notNullValue());

    OverdraftFee feePayload = overdraftFeeMessage.getPayload();
    assertThat(feePayload, notNullValue());
    assertThat(feePayload.accountNumber, equalTo(121212L));
    assertThat(feePayload.overdraftFee, equalTo(new BigDecimal("15.00")));

    overdrawn = new Overdrawn(33443344L, 656565L, new BigDecimal("-98.00"), new BigDecimal("-200.00"));
    overdrawnSource.send(overdrawn);

    await().atMost(3, TimeUnit.SECONDS).until(() -> overdraftSink.received().size() == 2);

    overdraftFeeMessage = overdraftSink.received().get(1);
    assertThat(overdraftFeeMessage, notNullValue());

    feePayload = overdraftFeeMessage.getPayload();
    assertThat(feePayload, notNullValue());
    assertThat(feePayload.accountNumber, equalTo(33443344L));
    assertThat(feePayload.overdraftFee, equalTo(new BigDecimal("15.00")));

    overdrawn = new Overdrawn(121212L, 212121L, new BigDecimal("-285.00"), new BigDecimal("-300.00"));
    overdrawnSource.send(overdrawn);

    await().atMost(3, TimeUnit.SECONDS).until(() -> overdraftSink.received().size() == 3);

    overdraftFeeMessage = overdraftSink.received().get(2);
    assertThat(overdraftFeeMessage, notNullValue());

    feePayload = overdraftFeeMessage.getPayload();
    assertThat(feePayload, notNullValue());
    assertThat(feePayload.accountNumber, equalTo(121212L));
    assertThat(feePayload.overdraftFee, equalTo(new BigDecimal("30.00")));

    overdrawn = new Overdrawn(878897L, 212121L, new BigDecimal("-76.00"), new BigDecimal("-200.00"));
    overdrawnSource.send(overdrawn);

    await().atMost(3, TimeUnit.SECONDS).until(() -> overdraftSink.received().size() == 4);

    overdraftFeeMessage = overdraftSink.received().get(3);
    assertThat(overdraftFeeMessage, notNullValue());

    feePayload = overdraftFeeMessage.getPayload();
    assertThat(feePayload, notNullValue());
    assertThat(feePayload.accountNumber, equalTo(878897L));
    assertThat(feePayload.overdraftFee, equalTo(new BigDecimal("35.00")));
  }

  @Test
  void testUpdateOverdraftEvent() {
    InMemorySink<OverdraftLimitUpdate> limitUpdateSink = connector.sink("update-overdraft");

    given()
        .contentType(ContentType.JSON)
        .body("-550.00")
        .when().put("/overdraft/{accountNumber}", 4324321)
        .then()
        .statusCode(204);

    assertThat(limitUpdateSink.received().size(), equalTo(1));

    Message<OverdraftLimitUpdate> updateEventMsg = limitUpdateSink.received().get(0);
    assertThat(updateEventMsg, notNullValue());

    OverdraftLimitUpdate updatePayload = updateEventMsg.getPayload();
    assertThat(updatePayload, notNullValue());
    assertThat(updatePayload.accountNumber, equalTo(4324321L));
    assertThat(updatePayload.newOverdraftLimit, equalTo(new BigDecimal("-550.00")));
  }
}
