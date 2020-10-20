package quarkus.overdraft;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import quarkus.overdraft.events.AccountFee;
import quarkus.overdraft.events.Overdrawn;
import quarkus.overdraft.model.CustomerOverdraft;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class ProcessOverdraftFee {
  @Incoming("customer-overdrafts")
  @Outgoing("overdraft-fee")
  public AccountFee processOverdraftFee(Message<Overdrawn> message) {
    Overdrawn payload = message.getPayload();
    CustomerOverdraft customerOverdraft = message.getMetadata(CustomerOverdraft.class).get();

    AccountFee feeEvent = new AccountFee();
    feeEvent.accountNumber = payload.accountNumber;
    feeEvent.overdraftFee = determineFee(payload.overdraftLimit, customerOverdraft.totalOverdrawnEvents,
        customerOverdraft.accountOverdrafts.get(payload.accountNumber).numberOverdrawnEvents);
    return feeEvent;
  }

  private BigDecimal determineFee(BigDecimal overdraftLimit, int customerOverdrawnTimes,
                                  int accountOverdrawnTimes) {
    return new BigDecimal((5 * accountOverdrawnTimes) + (10 * customerOverdrawnTimes))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
