package quarkus.accounts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/accounts",
                produces=MediaType.APPLICATION_JSON_VALUE,
                consumes=MediaType.APPLICATION_JSON_VALUE)
public class AccountResource {

  AccountRepository repository;

  public AccountResource(AccountRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public String hello() {
    return "hello";
  }

  @GetMapping("/{acctNumber}/balance")
  public BigDecimal getBalance(@PathVariable("acctNumber") Long accountNumber) {

    Account account = repository.findByAccountNumber(accountNumber);

    if (account == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with " + accountNumber + " does not exist.");
    }

    return account.getBalance();
  }

  @PostMapping("{acctNumber}/transaction")
  @Transactional
  public Map<String, List<String>> transact(@RequestHeader("Accept") String acceptHeader,
        @PathVariable("acctNumber") Long accountNumber,
        @RequestBody BigDecimal amount) {
    Account entity = repository.findByAccountNumber(accountNumber);

    if (entity == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with " + accountNumber + " does not exist.");
    }

    if (entity.accountStatus.equals(AccountStatus.OVERDRAWN)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is overdrawn, no further withdrawals permitted");
    }

    entity.setBalance(entity.getBalance().add(amount));
    repository.save(entity);

    List<String> list = new ArrayList<>();

    list.add((acceptHeader));
    Map<String,List<String>> map = new HashMap<String,List<String>>();
    map.put("Accept", list);

    return map;
  }

  @RestControllerAdvice
  public static class ErrorMapper {
    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<Object> toResponse(Exception exception) {

      HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
      if (exception instanceof ResponseStatusException) {
        code = ((ResponseStatusException) exception).getStatus();
      }

      JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
          .add("exceptionType", exception.getClass().getName())
          .add("code", code.value());

      if (exception.getMessage() != null) {
        entityBuilder.add("error", exception.getMessage());
      }

      return new ResponseEntity(entityBuilder.build(), code);
    }
  }
}
