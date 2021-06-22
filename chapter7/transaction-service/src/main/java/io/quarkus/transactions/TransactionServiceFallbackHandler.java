package io.quarkus.transactions;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.logging.Logger;

public class TransactionServiceFallbackHandler
       implements FallbackHandler<Response> {                           // <1>

    Logger LOG = Logger.getLogger(TransactionServiceFallbackHandler.class);

    @Override
    public Response handle(ExecutionContext context) {                  // <2>
        Response response;
        String name;

        if (context.getFailure().getCause() == null) {                  // <3>
             name = context.getFailure() .getClass().getSimpleName();
         } else {
            name = context.getFailure().getCause().getClass().getSimpleName();
         }

        switch (name) {
            case "BulkheadException":
                response = Response
                           .status(Response.Status.TOO_MANY_REQUESTS)   // <4>
                           .build();
                break;

            case "TimeoutException":
                response = Response
                           .status(Response.Status.GATEWAY_TIMEOUT)     // <5>
                           .build();
                break;

            case "CircuitBreakerOpenException":
                response = Response
                           .status(Response.Status.SERVICE_UNAVAILABLE) // <6>
                           .build();
                break;

            case "ResteasyWebApplicationException":
            case "WebApplicationException":
            case "HttpHostConnectException":
                response = Response
                           .status(Response.Status.BAD_GATEWAY)         // <7>
                           .build();
                break;

        default:
            response = Response
                       .status(Response.Status.NOT_IMPLEMENTED)
                       .build();

        }

        LOG.info("******** "
             +  context.getMethod().getName()
             + ": " + name
             + " ********");

        return response;
    }
}
