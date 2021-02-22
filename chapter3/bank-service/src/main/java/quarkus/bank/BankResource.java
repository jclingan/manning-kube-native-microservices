package quarkus.bank;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/bank")
public class BankResource {
    @ConfigProperty(name="bank.name", defaultValue = "Bank of Default")
    String name;

    @ConfigProperty(name="db.username", defaultValue = "Missing")
    String db_username;

    @ConfigProperty(name="db.password", defaultValue = "Missing")
    String db_password;

    BankSupportConfig config;

    public BankResource(BankSupportConfig config) {
            this.config = config;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/support")
    public Map<String, String> getSupport() {
        HashMap<String,String> map = new HashMap<>();

        map.put("email", config.email);
                map.put("phone", config.getPhone());

        return map;
    }
    @GET
    @Path("/name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName() {
        return name;
    }

    @ConfigProperty(name="app.mobilebanking")
    Optional<Boolean> mobileBanking;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/mobilebanking")
    public Boolean getMobileBanking() {
        return mobileBanking.orElse(false);
    }

    @ConfigProperty(name="username")
    String  username;

    @ConfigProperty(name="password")
    String password;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/secrets")
    public Map<String, String> getSecrets() {
        HashMap<String,String> map = new HashMap<>();

        map.put("username", username);
        map.put("password", password);
        map.put("db.username", db_username);
        map.put("db.password", db_password);

        return map;
    }
}