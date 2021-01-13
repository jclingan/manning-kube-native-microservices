package io.quarkus.transactions;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TransactionServiceFallbackHandler
       implements FallbackHandler<ResponseEntity<String>> {

    @Override
    public ResponseEntity<String> handle(ExecutionContext context) {
        Logger LOG = Logger.getLogger(TransactionServiceFallbackHandler.class);

        ResponseEntity<String> response;
        String name;

        if (context.getFailure().getCause() == null) {
             name = context.getFailure() .getClass().getSimpleName();
         } else {
            name = context.getFailure().getCause().getClass().getSimpleName();
         }

        switch (name) {
            case "BulkheadException":
                response = new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
                break;

            case "TimeoutException":
                response = new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
                break;

            case "CircuitBreakerOpenException":
                response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
                break;

            case "WebApplicationException":
            case "HttpHostConnectException":
                response = new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
                break;

        default:
                response = new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }

        LOG.info("******** "
             +  context.getMethod().getName()
             + ": " + name
             + " ********");

        return response;
    }
}