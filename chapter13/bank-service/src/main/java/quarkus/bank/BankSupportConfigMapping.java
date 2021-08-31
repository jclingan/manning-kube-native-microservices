package quarkus.bank;

import io.smallrye.config.ConfigMapping;

import javax.validation.constraints.Size;

@ConfigMapping(prefix = "bank-support-mapping")
public interface BankSupportConfigMapping {
    @Size(min=12, max=12)
    String phone();

    String email();

    Business business();

    interface Business {
        @Size(min=12, max=12)
        String phone();
        String email();
    }
}