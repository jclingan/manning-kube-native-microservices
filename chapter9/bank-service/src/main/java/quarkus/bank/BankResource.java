package quarkus.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bank")
public class BankResource {
    @Value("${bank.name:Bank of Default}")
    String name;

    @Value("${db.username:Missing}")
    String db_username;

    @Value("${db.password:Missing}")
    String db_password;

    @Value("app.mobilebanking")
    Optional<Boolean> mobileBanking;

    @Value("username")
    String username;

    @Value("password")
    String password;

    BankSupportConfig config;

    public BankResource(BankSupportConfig config) {
        this.config = config;
    }

    @GetMapping("/support")
    public Map<String, String> getSupport() {
        HashMap<String, String> map = new HashMap<>();

        map.put("email", config.email);
        map.put("phone", config.getPhone());

        return map;
    }

    @GetMapping(value = "/name", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getName() {
        return name;
    }

    @GetMapping(value = "/mobilebanking", produces = MediaType.TEXT_PLAIN_VALUE)
    public Boolean getMobileBanking() {
        return mobileBanking.orElse(false);
    }

    @GetMapping("/secrets")
    public Map<String, String> getSecrets() {
        HashMap<String, String> map = new HashMap<>();

        map.put("username", username);
        map.put("password", password);
        map.put("db.username", db_username);
        map.put("db.password", db_password);

        return map;
    }
}
