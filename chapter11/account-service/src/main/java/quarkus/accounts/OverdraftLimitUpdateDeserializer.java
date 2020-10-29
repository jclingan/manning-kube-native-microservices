package quarkus.accounts;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import quarkus.accounts.events.OverdraftLimitUpdate;

public class OverdraftLimitUpdateDeserializer extends JsonbDeserializer<OverdraftLimitUpdate> {
  public OverdraftLimitUpdateDeserializer() {
    super(OverdraftLimitUpdate.class);
  }
}
