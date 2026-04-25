package com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;

@Primary
@Component
public class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String password) {
        return "hashed-" + password;
    }

    @Override
    public boolean matches(String raw, String hash) {
        return hash.equals("hashed-" + raw);
    }
}
