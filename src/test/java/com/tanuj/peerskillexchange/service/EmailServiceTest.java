package com.tanuj.peerskillexchange.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailServiceTest {
    @Test
    void unavailableMailDoesNotPretendToSendVerification() {
        EmailService service = new EmailService(null, "no-reply@example.test");
        assertThrows(IllegalStateException.class,
                () -> service.sendVerificationCode("alice@example.test", "Verify", "123456", 10));
    }
}
