package quarkus.overdraft;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import quarkus.overdraft.events.Overdrawn;

public class OverdrawnDeserializer extends JsonbDeserializer<Overdrawn> {
  public OverdrawnDeserializer() {
    super(Overdrawn.class);
  }
}
