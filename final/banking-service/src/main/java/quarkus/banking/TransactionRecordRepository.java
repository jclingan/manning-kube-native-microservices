package quarkus.banking;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TransactionRecordRepository implements PanacheRepository<TransactionRecord> {
  public BigDecimal totalDailyWithdrawals(Long accountNumber) {
    List<TransactionRecord> records = find("accountNumber = ?1 and creationDate = ?1", accountNumber, LocalDate.now()).list();
    return records.stream()
        .map(TransactionRecord::getAmount)
        .filter(amt -> amt.compareTo(BigDecimal.ZERO) < 0)
        .reduce(BigDecimal.ZERO, BigDecimal::subtract);
  }
}
