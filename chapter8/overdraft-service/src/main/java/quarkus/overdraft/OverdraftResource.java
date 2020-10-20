package quarkus.overdraft;

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

        return message.addMetadata(customerOverdraft);
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
    @Channel("update-overdraft")
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