package io.quarkus.transactions;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.lang.reflect.Method;

public class AccountRequestFilter implements ClientRequestFilter {
  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    Method invokedMethod = (Method) requestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
    requestContext.getHeaders().add("Invoked-Client-Method", invokedMethod.getName());
  }
}
