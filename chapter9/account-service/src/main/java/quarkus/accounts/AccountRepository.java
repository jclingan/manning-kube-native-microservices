package quarkus.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  public Account findByAccountNumber(Long accountNumber);
}
