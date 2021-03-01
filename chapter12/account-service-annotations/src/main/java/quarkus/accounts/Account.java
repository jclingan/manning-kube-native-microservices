package quarkus.accounts;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;

@Schema(name = "Account", description = "POJO representing an account.", type = SchemaType.OBJECT)
public class Account {
  @Schema(required = true, example = "123456789", minLength = 8, type = SchemaType.INTEGER)
  public Long accountNumber;
  @Schema(required = true, example = "432542374", minLength = 6, type = SchemaType.INTEGER)
  public Long customerNumber;
  @Schema(example = "Steve Hanger", type = SchemaType.STRING)
  public String customerName;
  @Schema(required = true, example = "438.32")
  public BigDecimal balance;
  @Schema(required = true, example = "OPEN")
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

