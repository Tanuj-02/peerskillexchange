package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.user.UserRequest;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    private static User user() {
        User user = new User();
        user.setId("alice1");
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setRole(Role.USER);
        return user;
    }

    private static UserService service(UserRepository users) {
        return new UserService(users, mock(BCryptPasswordEncoder.class), mock(EmailService.class),
                mock(UserSkillService.class), mock(UserSkillPageLoader.class));
    }

    @Test
    void publicProfileDoesNotExposeEmailOrRole() {
        UserRepository users = mock(UserRepository.class);
        when(users.findById("alice1")).thenReturn(Optional.of(user()));
        var result = service(users).getById("alice1");
        assertNull(result.email());
        assertNull(result.role());
    }

    @Test
    void genericProfileUpdateRejectsUnverifiedEmailChange() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));
        assertThrows(IllegalArgumentException.class, () -> service(users).updateUser(
                new UserRequest("alice1", "Alice", "alice", "new@example.test", null, null, null, null)));
        verify(users, never()).save(any());
    }

    @Test
    void genericProfileUpdateRejectsUsernameChange() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));
        assertThrows(IllegalArgumentException.class, () -> service(users).updateUser(
                new UserRequest("alice1", "Alice", "other", "alice@example.test", null, null, null, null)));
        verify(users, never()).save(any());
    }

    @Test
    void emailChangeRequiresTheCodeSentToTheNewAddress() {
        User user = user();
        UserRepository users = mock(UserRepository.class);
        EmailService mail = mock(EmailService.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(users.findByEmail("new@example.test")).thenReturn(Optional.empty());
        UserService service = new UserService(users, new BCryptPasswordEncoder(), mail,
                mock(UserSkillService.class), mock(UserSkillPageLoader.class));

        service.requestEmailChange("alice", "NEW@example.test");
        assertEquals("alice@example.test", user.getEmail());
        assertEquals("new@example.test", user.getPendingEmail());
        assertNotNull(user.getPendingEmailCodeHash());
        verify(mail).sendVerificationCode(eq("new@example.test"), anyString(), anyString(), eq(10));

        assertThrows(IllegalArgumentException.class, () -> service.verifyEmailChange("alice", "000000"));
        assertEquals(1, user.getPendingEmailAttempts());
        assertEquals("alice@example.test", user.getEmail());
    }

    @Test
    void validEmailCodeUpdatesAddressAndCannotBeReused() {
        User user = user();
        user.setPendingEmail("new@example.test");
        user.setPendingEmailCodeHash(new BCryptPasswordEncoder().encode("123456"));
        user.setPendingEmailExpiry(LocalDateTime.now().plusMinutes(5));
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        UserService service = new UserService(users, new BCryptPasswordEncoder(), mock(EmailService.class),
                mock(UserSkillService.class), mock(UserSkillPageLoader.class));

        service.verifyEmailChange("alice", "123456");
        assertEquals("new@example.test", user.getEmail());
        assertNull(user.getPendingEmailCodeHash());
        assertThrows(IllegalArgumentException.class, () -> service.verifyEmailChange("alice", "123456"));
    }

    @Test
    void passwordCodeIsSingleUseAndRequiresCurrentPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = user();
        user.setPassword(encoder.encode("oldpass99"));
        user.setPasswordChangeCode(encoder.encode("123456"));
        user.setPasswordChangeCodeExpiry(LocalDateTime.now().plusMinutes(5));
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        UserService service = new UserService(users, encoder, mock(EmailService.class),
                mock(UserSkillService.class), mock(UserSkillPageLoader.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.updatePassword("alice", "wrongpass", "newpass99", "123456"));
        service.updatePassword("alice", "oldpass99", "newpass99", "123456");
        assertTrue(encoder.matches("newpass99", user.getPassword()));
        assertNull(user.getPasswordChangeCode());
        assertThrows(IllegalArgumentException.class,
                () -> service.updatePassword("alice", "newpass99", "another99", "123456"));
    }

    @Test
    void duplicateEmailCannotStartVerification() {
        UserRepository users = mock(UserRepository.class);
        EmailService mail = mock(EmailService.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user()));
        when(users.findByEmail("taken@example.test")).thenReturn(Optional.of(new User()));
        UserService service = new UserService(users, new BCryptPasswordEncoder(), mail,
                mock(UserSkillService.class), mock(UserSkillPageLoader.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.requestEmailChange("alice", "taken@example.test"));
        verifyNoInteractions(mail);
    }
}
