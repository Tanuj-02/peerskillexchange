package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.model.Conversation;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ChatMessageRepository;
import com.tanuj.peerskillexchange.repository.ConversationRepository;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    @Test
    void rejectsSendingWhenAnOldConversationIsNoLongerConnected() {
        User alice = new User(); alice.setId("alice1"); alice.setUsername("alice");
        User bob = new User(); bob.setId("bob001"); bob.setUsername("bob");
        Conversation conversation = new Conversation();
        conversation.setUser1(alice); conversation.setUser2(bob);

        UserRepository users = mock(UserRepository.class);
        ExchangeRequestRepository requests = mock(ExchangeRequestRepository.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversations.findById(3L)).thenReturn(Optional.of(conversation));

        ChatService service = new ChatService(users, requests, conversations, messages);
        assertThrows(AccessDeniedException.class,
                () -> service.sendMessageToConversation("alice", 3L, "hello"));
        verify(messages, never()).save(any());
    }

    @Test
    void rejectsSubscriptionToAnotherUsersConversation() {
        User alice = new User(); alice.setId("alice1"); alice.setUsername("alice");
        User bob = new User(); bob.setId("bob001");
        User carol = new User(); carol.setId("carol1");
        Conversation conversation = new Conversation();
        conversation.setUser1(bob); conversation.setUser2(carol);
        UserRepository users = mock(UserRepository.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversations.findById(3L)).thenReturn(Optional.of(conversation));
        ChatService service = new ChatService(users, mock(ExchangeRequestRepository.class),
                conversations, mock(ChatMessageRepository.class));
        assertThrows(AccessDeniedException.class,
                () -> service.assertConversationAccess("alice", 3L));
    }

    @Test
    void rejectsSubscriptionAfterConnectionEnds() {
        User alice = new User(); alice.setId("alice1"); alice.setUsername("alice");
        User bob = new User(); bob.setId("bob001");
        Conversation conversation = new Conversation();
        conversation.setUser1(alice); conversation.setUser2(bob);
        UserRepository users = mock(UserRepository.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversations.findById(3L)).thenReturn(Optional.of(conversation));
        ChatService service = new ChatService(users, mock(ExchangeRequestRepository.class),
                conversations, mock(ChatMessageRepository.class));
        assertThrows(AccessDeniedException.class,
                () -> service.assertConversationAccess("alice", 3L));
    }
}
