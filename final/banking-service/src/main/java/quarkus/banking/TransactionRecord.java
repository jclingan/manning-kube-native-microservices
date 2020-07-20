package quarkus.banking;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class TransactionRecord {
  @Id
  @GeneratedValue
  private Long id;

  private Long accountNumber;
  private BigDecimal amount;
  private LocalDate creationDate;
  private LocalTime creationTime;

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

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(LocalDate creationDate) {
    this.creationDate = creationDate;
  }

  public LocalTime getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(LocalTime creationTime) {
    this.creationTime = creationTime;
  }
}
