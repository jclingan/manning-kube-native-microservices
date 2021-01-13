package quarkus.bank;

import javax.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class BankSupportConfig {
    @Size(min=12, max = 12)   // xxx-xxx-xxxx format
    private String phone;

    public String email;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
