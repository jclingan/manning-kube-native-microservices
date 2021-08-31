package quarkus.overdraft;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.*;
import quarkus.overdraft.events.OverdraftLimitUpdate;
import quarkus.overdraft.events.Overdrawn;
import quarkus.overdraft.model.AccountOverdraft;
import quarkus.overdraft.model.CustomerOverdraft;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Path("/overdraft")
public class OverdraftResource {

    private final Map<Long, CustomerOverdraft> customerOverdrafts = new HashMap<>();

    @Inject
    Tracer tracer;

    @Incoming("account-overdrawn")
    @Outgoing("customer-overdrafts")
    public Message<Overdrawn> overdraftNotification(Message<Overdrawn> message) {
        Overdrawn overdrawnPayload = message.getPayload();

        CustomerOverdraft customerOverdraft = customerOverdrafts.get(overdrawnPayload.customerNumber);

        if (customerOverdraft == null) {
            customerOverdraft = new CustomerOverdraft();
            customerOverdraft.customerNumber = overdrawnPayload.customerNumber;

            customerOverdrafts.put(overdrawnPayload.customerNumber, customerOverdraft);
        }

        AccountOverdraft accountOverdraft = customerOverdraft.accountOverdrafts.get(overdrawnPayload.accountNumber);
        if (accountOverdraft == null) {
            accountOverdraft = new AccountOverdraft();
            accountOverdraft.accountNumber = overdrawnPayload.accountNumber;

            customerOverdraft.accountOverdrafts.put(overdrawnPayload.accountNumber, accountOverdraft);
        }

        customerOverdraft.totalOverdrawnEvents++;
        accountOverdraft.currentOverdraft = overdrawnPayload.overdraftLimit;
        accountOverdraft.numberOverdrawnEvents++;

        RecordHeaders headers = new RecordHeaders();
        if (message.getMetadata(IncomingKafkaRecordMetadata.class).isPresent()) {
            Span span = tracer.buildSpan("process-overdraft-fee")
                .asChildOf(TracingKafkaUtils.extractSpanContext(message.getMetadata(IncomingKafkaRecordMetadata.class).get().getHeaders(), tracer))
                .start();
            try (Scope scope = tracer.activateSpan(span)) {
                TracingKafkaUtils.inject(span.context(), headers, tracer);
            } finally {
                span.finish();
            }
        }
        OutgoingKafkaRecordMetadata<Object> kafkaMetadata = OutgoingKafkaRecordMetadata.builder()
            .withHeaders(headers)
            .build();

        return message.addMetadata(customerOverdraft).addMetadata(kafkaMetadata);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public List<AccountOverdraft> retrieveAllAccountOverdrafts() {
        return customerOverdrafts.values()
            .stream()
            .flatMap(co -> co.accountOverdrafts.values().stream())
            .collect(Collectors.toList());
    }

    @Inject
    @Channel("overdraft-update")
    Emitter<OverdraftLimitUpdate> emitter;

    @PUT
    @Path("/{accountNumber}")
    public void updateAccountOverdraft(@PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
        OverdraftLimitUpdate updateEvent = new OverdraftLimitUpdate();
        updateEvent.accountNumber = accountNumber;
        updateEvent.newOverdraftLimit = amount;

        emitter.send(updateEvent);
    }
}