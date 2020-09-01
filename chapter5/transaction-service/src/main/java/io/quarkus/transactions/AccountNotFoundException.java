package io.quarkus.transactions;

public class AccountNotFoundException extends Exception {
  public AccountNotFoundException(String message) {
    super(message);
  }
}
