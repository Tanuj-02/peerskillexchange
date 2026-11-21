package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExchangeRequestServiceTest {
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void aRejectedHistoryDoesNotPreventAnewRequest() {
        User alice = new User(); alice.setId("alice1"); alice.setUsername("alice"); alice.setRole(Role.USER);
        User bob = new User(); bob.setId("bob001"); bob.setUsername("bob"); bob.setRole(Role.USER);
        UserRepository users = mock(UserRepository.class);
        ExchangeRequestRepository requests = mock(ExchangeRequestRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findById("bob001")).thenReturn(Optional.of(bob));
        when(users.lockById("alice1")).thenReturn(Optional.of(alice));
        when(users.lockById("bob001")).thenReturn(Optional.of(bob));
        when(requests.existsConnectionBetween(alice, bob, RequestStatus.REJECTED)).thenReturn(true);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SkillRepository skills = mock(SkillRepository.class);
        when(skills.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));

        ExchangeRequestService service = new ExchangeRequestService(requests, users,
                skills, mock(ConversationRepository.class),
                mock(ChatMessageRepository.class), mock(NotificationService.class));
        var response = service.createExchangeRequest(
                new ERRequest("bob001", "Swap?", List.of("Java"), List.of("React")));
        assertEquals("PENDING", response.status());
        assertEquals(List.of("java"), response.canTeach());
        assertEquals(List.of("react"), response.wantToLearn());
    }

    @Test
    void acceptedOrPendingConnectionBlocksANewRequest() {
        User alice = new User(); alice.setId("alice1"); alice.setUsername("alice");
        User bob = new User(); bob.setId("bob001"); bob.setUsername("bob");
        UserRepository users = mock(UserRepository.class);
        ExchangeRequestRepository requests = mock(ExchangeRequestRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findById("bob001")).thenReturn(Optional.of(bob));
        when(users.lockById("alice1")).thenReturn(Optional.of(alice));
        when(users.lockById("bob001")).thenReturn(Optional.of(bob));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));
        ExchangeRequestService service = new ExchangeRequestService(requests, users,
                mock(SkillRepository.class), mock(ConversationRepository.class),
                mock(ChatMessageRepository.class), mock(NotificationService.class));

        when(requests.existsConnectionBetween(alice, bob, RequestStatus.ACCEPTED)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.createExchangeRequest(
                new ERRequest("bob001", "Swap?", List.of(), List.of())));
        when(requests.existsConnectionBetween(alice, bob, RequestStatus.ACCEPTED)).thenReturn(false);
        when(requests.existsConnectionBetween(alice, bob, RequestStatus.PENDING)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.createExchangeRequest(
                new ERRequest("bob001", "Swap?", List.of(), List.of())));
        verify(requests, never()).save(any());
    }
}
