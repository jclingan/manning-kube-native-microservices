package quarkus.overdraft;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class OverDraft {
  @Id
  @GeneratedValue
  private Long id;
}
