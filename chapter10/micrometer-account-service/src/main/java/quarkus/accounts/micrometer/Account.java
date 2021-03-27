package quarkus.accounts.micrometer;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
public class Account extends PanacheEntity {
  public Long accountNumber;
  public Long customerNumber;
  public String customerName;
  public BigDecimal balance;
  public AccountStatus accountStatus = AccountStatus.OPEN;

  public static long totalAccountsForCustomer(Long customerNumber) {
    return find("customerNumber", customerNumber).count();
  }

  public static Account findByAccountNumber(Long accountNumber) {
    return find("accountNumber", accountNumber).firstResult();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return id.equals(account.id) &&
        accountNumber.equals(account.accountNumber) &&
        customerNumber.equals(account.customerNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, accountNumber, customerNumber);
  }
}
