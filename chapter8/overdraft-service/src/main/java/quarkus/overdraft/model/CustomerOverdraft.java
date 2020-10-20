package quarkus.overdraft.model;

import java.util.HashMap;
import java.util.Map;

public class CustomerOverdraft {
  public Long customerNumber;
  public int totalOverdrawnEvents = 0;
  public Map<Long, AccountOverdraft> accountOverdrafts = new HashMap<>();
}
