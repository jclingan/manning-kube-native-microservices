package quarkus.overdraft.events;

import java.math.BigDecimal;

public class Overdrawn {
  public Long accountNumber;
  public Long customerNumber;
  public BigDecimal balance;
  public BigDecimal overdraftLimit;

  public Overdrawn() {
  }

  public Overdrawn(Long accountNumber, Long customerNumber, BigDecimal balance, BigDecimal overdraftLimit) {
    this.accountNumber = accountNumber;
    this.customerNumber = customerNumber;
    this.balance = balance;
    this.overdraftLimit = overdraftLimit;
  }
}
