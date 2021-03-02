package quarkus.accounts;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
  public Long accountNumber;
  public Long customerNumber;
  public String customerName;
  public BigDecimal balance;
  public AccountStatus accountStatus = AccountStatus.OPEN;

  public Account() {
  }

  public Account(Long accountNumber, Long customerNumber, String customerName, BigDecimal balance) {
    this.accountNumber = accountNumber;
    this.customerNumber = customerNumber;
    this.customerName = customerName;
    this.balance = balance;
  }

  public void markOverdrawn() {
    accountStatus = AccountStatus.OVERDRAWN;
  }

  public void removeOverdrawnStatus() {
    accountStatus = AccountStatus.OPEN;
  }

  public void close() {
    accountStatus = AccountStatus.CLOSED;
    balance = BigDecimal.valueOf(0);
  }

  public void withdrawFunds(BigDecimal amount) {
    balance = balance.subtract(amount);
  }

  public void addFunds(BigDecimal amount) {
    balance = balance.add(amount);
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Long getAccountNumber() {
    return accountNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return accountNumber.equals(account.accountNumber) &&
        customerNumber.equals(account.customerNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountNumber, customerNumber);
  }
}

