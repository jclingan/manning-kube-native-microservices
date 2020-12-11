package quarkus.overdraft;

import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.Header;
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
import java.nio.charset.StandardCharsets;
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

        SpanContext parentSpan = tracer.extract(Format.Builtin.TEXT_MAP, new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                Optional<IncomingKafkaRecordMetadata> metadata = message.getMetadata(IncomingKafkaRecordMetadata.class);
                if (metadata.isPresent()) {
                    Map<String, String> map = new HashMap<>();
                    for (Header header : metadata.get().getHeaders()) {
                        map.put(header.key(), header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
                    }
                    return map.entrySet().iterator();
                }
                return Collections.EMPTY_SET.iterator();
            }

            @Override
            public void put(String key, String value) {
                throw new UnsupportedOperationException("This should only be used with Tracer.extract()");
            }
        });

        RecordHeaders headers = new RecordHeaders();
        try (Scope scope = tracer.buildSpan("process-overdraft-fee")
                .asChildOf(parentSpan)
                .startActive(true)) {
            tracer.inject(scope.span().context(), Format.Builtin.TEXT_MAP, new TextMap() {
                @Override
                public Iterator<Map.Entry<String, String>> iterator() {
                    throw new UnsupportedOperationException("This should only be used with Tracer.inject()");
                }

                @Override
                public void put(String key, String value) {
                    headers.add(key, value.getBytes(StandardCharsets.UTF_8));
                }
            });
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