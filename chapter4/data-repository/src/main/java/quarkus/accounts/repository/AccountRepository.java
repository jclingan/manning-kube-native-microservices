package quarkus.accounts.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {
  public Account findByAccountNumber(Long accountNumber) {
    return find("accountNumber = ?1", accountNumber).firstResult();
  }
}
