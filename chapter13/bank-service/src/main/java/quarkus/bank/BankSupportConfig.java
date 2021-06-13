package quarkus.bank;

import io.quarkus.arc.config.ConfigProperties;

import javax.validation.constraints.Size;

@ConfigProperties
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
