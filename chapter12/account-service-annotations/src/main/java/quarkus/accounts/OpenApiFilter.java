package quarkus.accounts;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Operation;

import java.util.List;

public class OpenApiFilter implements OASFilter {
  @Override
  public Operation filterOperation(Operation operation) {
    if (operation.getOperationId().equals("closeAccount")) {
      operation.setTags(List.of("close-account"));
    }
    return operation;
  }
}
