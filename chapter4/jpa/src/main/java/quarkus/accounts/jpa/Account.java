package quarkus.accounts.jpa;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@NamedQuery(name = "Accounts.findAll", query = "SELECT a FROM Account a ORDER BY a.accountNumber")
@NamedQuery(name = "Accounts.findByAccountNumber", query = "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber ORDER BY a.accountNumber")
public class Account {
  @Id
  @SequenceGenerator(name = "accountsSequence", sequenceName = "accounts_id_seq", allocationSize = 1, initialValue = 10)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountsSequence")
  private Long id;

  private Long accountNumber;
  private Long customerNumber;
  private String customerName;
  private BigDecimal balance;
  private AccountStatus accountStatus = AccountStatus.OPEN;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(Long accountNumber) {
    this.accountNumber = accountNumber;
  }

  public Long getCustomerNumber() {
    return customerNumber;
  }

  public void setCustomerNumber(Long customerNumber) {
    this.customerNumber = customerNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(AccountStatus accountStatus) {
    this.accountStatus = accountStatus;
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

