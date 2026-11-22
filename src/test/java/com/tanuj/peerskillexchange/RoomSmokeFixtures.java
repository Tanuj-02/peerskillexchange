package com.tanuj.peerskillexchange;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@Profile("whiteboard-smoke")
public class RoomSmokeFixtures {
    @Bean
    ApplicationRunner seedWhiteboardUsers(UserRepository users, ExchangeRequestRepository exchanges,
                                          BCryptPasswordEncoder encoder) {
        return args -> {
            User alice = create(users, encoder, "smk001", "whiteboard_alice", "Alice");
            User bob = create(users, encoder, "smk002", "whiteboard_bob", "Bob");
            create(users, encoder, "smk003", "whiteboard_stranger", "Stranger");
            if (!exchanges.existsConnectionBetween(alice, bob, RequestStatus.ACCEPTED)) {
                ExchangeRequest exchange = new ExchangeRequest();
                exchange.setSender(alice);
                exchange.setReceiver(bob);
                exchange.setStatus(RequestStatus.ACCEPTED);
                exchanges.save(exchange);
            }
        };
    }

    private User create(UserRepository users, BCryptPasswordEncoder encoder,
                        String id, String username, String name) {
        return users.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setId(id); user.setUsername(username); user.setName(name);
            user.setEmail(username + "@example.test");
            user.setPassword(encoder.encode("WhiteboardDemo123!"));
            user.setRole(Role.USER); user.setEmailVerified(true);
            return users.save(user);
        });
    }
}
