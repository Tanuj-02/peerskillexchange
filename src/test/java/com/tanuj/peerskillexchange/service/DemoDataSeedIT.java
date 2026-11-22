package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.auth.Login;
import com.tanuj.peerskillexchange.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Run explicitly with -Dtest=DemoDataSeedIT after loading the local .env. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"board.kafka.enabled=false", "spring.kafka.listener.auto-startup=false"})
class DemoDataSeedIT {
    @Autowired DataSource dataSource;
    @Autowired UserRepository users;
    @Autowired SkillRepository skills;
    @Autowired UserSkillRepository userSkills;
    @Autowired ExchangeRequestRepository requests;
    @Autowired ConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired LearningRoomRepository rooms;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AuthService authService;

    @Test
    void seedOnlyConfiguredLocalMysqlAndPreserveExistingAccounts() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.matches("(?i)^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):3306/peerskillexchange(?:\\?.*)?$"),
                    "Demo seeding is restricted to local peerskillexchange MySQL.");
        }
        Map<String, String> originalPasswords = users.findAll().stream()
                .filter(user -> !user.getUsername().startsWith("demo_"))
                .collect(Collectors.toMap(user -> user.getId(), user -> user.getPassword()));
        var seeder = new DemoDataSeeder(users, skills, userSkills, requests, conversations,
                messages, rooms, encoder, new TransactionTemplate(transactionManager));
        var result = seeder.seed();
        assertEquals(originalPasswords.size() + 6, users.count());
        for (var entry : originalPasswords.entrySet()) {
            assertEquals(entry.getValue(), users.findById(entry.getKey()).orElseThrow().getPassword());
        }
        assertTrue(encoder.matches(DemoDataSeeder.DEMO_PASSWORD,
                users.findByUsername("demo_aarav").orElseThrow().getPassword()));
        for (String username : List.of("demo_aarav", "demo_meera", "demo_isha",
                "demo_kabir", "demo_riya", "demo_dev")) {
            var login = authService.login(new Login(username, DemoDataSeeder.DEMO_PASSWORD));
            assertEquals(username, login.username());
            assertFalse(login.token().isBlank());
        }
        assertFalse(seeder.seed().inserted());
        System.out.printf("Demo seed %s: users=%d newSkills=%d requests=%d rooms=%d messages=%d%n",
                result.inserted() ? "created" : "already present", result.users(), result.skills(),
                result.requests(), result.rooms(), result.messages());
    }
}
