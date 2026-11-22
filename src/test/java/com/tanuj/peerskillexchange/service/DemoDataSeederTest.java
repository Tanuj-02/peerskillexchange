package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:pse_demo_seed;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1")
class DemoDataSeederTest {
    @Autowired UserRepository users;
    @Autowired SkillRepository skills;
    @Autowired UserSkillRepository userSkills;
    @Autowired ExchangeRequestRepository requests;
    @Autowired ConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired LearningRoomRepository rooms;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void createsConnectedDemoAccountsWithHashedPasswordAndRerunsSafely() {
        var seeder = new DemoDataSeeder(users, skills, userSkills, requests, conversations,
                messages, rooms, encoder, new TransactionTemplate(transactionManager));
        var first = seeder.seed();
        assertTrue(first.inserted());
        assertEquals(6, first.users());
        assertEquals(6, first.requests());
        assertEquals(3, first.rooms());
        assertEquals(6, first.messages());
        assertEquals(3, requests.findAll().stream()
                .filter(request -> request.getStatus() == RequestStatus.ACCEPTED).count());
        assertEquals(2, requests.findAll().stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDING).count());
        assertEquals(1, requests.findAll().stream()
                .filter(request -> request.getStatus() == RequestStatus.REJECTED).count());
        for (var user : users.findAll()) {
            assertTrue(user.isEmailVerified());
            assertNotEquals(DemoDataSeeder.DEMO_PASSWORD, user.getPassword());
            assertTrue(encoder.matches(DemoDataSeeder.DEMO_PASSWORD, user.getPassword()));
        }
        assertFalse(seeder.seed().inserted());
        assertEquals(6, users.count());
        assertEquals(6, requests.count());
        assertEquals(3, rooms.count());
        assertEquals(6, messages.count());
    }
}
