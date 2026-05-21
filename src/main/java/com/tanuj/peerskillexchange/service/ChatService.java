package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.chat.ChatMessageResponse;
import com.tanuj.peerskillexchange.dto.chat.ConversationResponse;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.model.ChatMessage;
import com.tanuj.peerskillexchange.model.Conversation;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ChatMessageRepository;
import com.tanuj.peerskillexchange.repository.ConversationRepository;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            UserRepository userRepository,
            ExchangeRequestRepository exchangeRequestRepository,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.userRepository = userRepository;
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    private static boolean isBefore(User a, User b) {
        return a.getId().compareTo(b.getId()) < 0;
    }

    private Conversation getOrCreateConversation(User a, User b) {
        User u1 = isBefore(a, b) ? a : b;
        User u2 = isBefore(a, b) ? b : a;
        Optional<Conversation> existing = conversationRepository.findByUser1AndUser2(u1, u2);
        return existing.orElseGet(() -> {
            Conversation c = new Conversation();
            c.setUser1(u1);
            c.setUser2(u2);
            return conversationRepository.save(c);
        });
    }

    private void assertConnected(User me, User other) {
        boolean ok = exchangeRequestRepository.existsConnectionBetween(me, other, RequestStatus.ACCEPTED);
        if (!ok) {
            throw new IllegalArgumentException("Users are not connected yet (request not accepted).");
        }
    }

    @Transactional
    public ChatMessageResponse sendMessage(String senderUsername, String receiverId, String content) {
        if (content == null || content.trim().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Message content too long (max 2000 chars).");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot message yourself.");
        }

        assertConnected(sender, receiver);

        Conversation conversation = getOrCreateConversation(sender, receiver);
        ChatMessage msg = new ChatMessage();
        msg.setConversation(conversation);
        msg.setSender(sender);
        msg.setContent(trimmed);

        ChatMessage saved = chatMessageRepository.save(msg);
        return new ChatMessageResponse(
                saved.getId(),
                conversation.getId(),
                sender.getId(),
                sender.getUsername(),
                receiver.getId(),
                receiver.getUsername(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public ChatMessageResponse sendMessageToConversation(String senderUsername, Long conversationId, String content) {
        if (content == null || content.trim().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Message content too long (max 2000 chars).");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isMember = conversation.getUser1().getId().equals(sender.getId()) || conversation.getUser2().getId().equals(sender.getId());
        if (!isMember) {
            throw new IllegalArgumentException("Unauthorized to send message in this conversation.");
        }

        User receiver = conversation.getUser1().getId().equals(sender.getId())
                ? conversation.getUser2()
                : conversation.getUser1();

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conversation);
        msg.setSender(sender);
        msg.setContent(trimmed);

        ChatMessage saved = chatMessageRepository.save(msg);
        return new ChatMessageResponse(
                saved.getId(),
                conversation.getId(),
                sender.getId(),
                sender.getUsername(),
                receiver.getId(),
                receiver.getUsername(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listMyConversations(String username, Pageable pageable) {
        User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return conversationRepository.findForUser(me, pageable).map(c -> {
            User other = c.getUser1().getId().equals(me.getId()) ? c.getUser2() : c.getUser1();
            return new ConversationResponse(
                    c.getId(),
                    other.getId(),
                    other.getUsername(),
                    other.getName(),
                    other.getProfilePicture(),
                    c.getCreatedAt()
            );
        });
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(String username, Long conversationId, Pageable pageable) {
        User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isMember = conversation.getUser1().getId().equals(me.getId()) || conversation.getUser2().getId().equals(me.getId());
        if (!isMember) {
            throw new IllegalArgumentException("Unauthorized to view this conversation.");
        }

        return chatMessageRepository.findByConversation(conversation, pageable)
                .map(m -> {
                    User receiver = conversation.getUser1().getId().equals(m.getSender().getId())
                            ? conversation.getUser2()
                            : conversation.getUser1();
                    return new ChatMessageResponse(
                            m.getId(),
                            conversation.getId(),
                            m.getSender().getId(),
                            m.getSender().getUsername(),
                            receiver.getId(),
                            receiver.getUsername(),
                            m.getContent(),
                            m.getCreatedAt()
                    );
                });
    }
}

