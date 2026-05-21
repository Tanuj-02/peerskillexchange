package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.chat.ChatMessageResponse;
import com.tanuj.peerskillexchange.dto.notification.NotificationResponse;
import com.tanuj.peerskillexchange.enums.NotificationType;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.Notification;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.NotificationRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final int BODY_PREVIEW_MAX = 200;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public static NotificationResponse toDto(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.isViewed(),
                n.getActorUserId(),
                n.getActorUsername(),
                n.getConversationId(),
                n.getExchangeRequestId(),
                n.getChatMessageId(),
                n.getCreatedAt()
        );
    }

    private static String preview(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        return s.length() <= BODY_PREVIEW_MAX ? s : s.substring(0, BODY_PREVIEW_MAX) + "…";
    }

    @Transactional
    public void notifyChatMessage(ChatMessageResponse message) {
        User recipient = userRepository.findByUsername(message.receiverUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(NotificationType.CHAT_MESSAGE);
        n.setTitle("New message from " + message.senderUsername());
        n.setBody(preview(message.content()));
        n.setActorUserId(message.senderId());
        n.setActorUsername(message.senderUsername());
        n.setConversationId(message.conversationId());
        n.setChatMessageId(message.id());

        Notification saved = notificationRepository.save(n);
        pushToUser(recipient.getUsername(), toDto(saved));
    }

    @Transactional
    public void notifyExchangeRequestReceived(ExchangeRequest request) {
        User recipient = request.getReceiver();
        User sender = request.getSender();
        if (recipient == null || sender == null) {
            return;
        }

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(NotificationType.EXCHANGE_REQUEST_RECEIVED);
        n.setTitle("New skill exchange request");
        n.setBody(preview(sender.getName() + " (" + sender.getUsername() + ") sent you a request."));
        n.setActorUserId(sender.getId());
        n.setActorUsername(sender.getUsername());
        n.setExchangeRequestId(request.getId());

        Notification saved = notificationRepository.save(n);
        pushToUser(recipient.getUsername(), toDto(saved));
    }

    @Transactional
    public void notifyExchangeAccepted(ExchangeRequest request) {
        User sender = request.getSender();
        User receiver = request.getReceiver();
        if (sender == null || receiver == null) {
            return;
        }

        Notification n = new Notification();
        n.setRecipient(sender);
        n.setType(NotificationType.EXCHANGE_ACCEPTED);
        n.setTitle("Request accepted");
        n.setBody(receiver.getName() + " (" + receiver.getUsername() + ") accepted your exchange request.");
        n.setActorUserId(receiver.getId());
        n.setActorUsername(receiver.getUsername());
        n.setExchangeRequestId(request.getId());

        Notification saved = notificationRepository.save(n);
        pushToUser(sender.getUsername(), toDto(saved));
    }

    @Transactional
    public void notifyExchangeRejected(ExchangeRequest request) {
        User sender = request.getSender();
        User receiver = request.getReceiver();
        if (sender == null || receiver == null) {
            return;
        }

        Notification n = new Notification();
        n.setRecipient(sender);
        n.setType(NotificationType.EXCHANGE_REJECTED);
        n.setTitle("Request declined");
        n.setBody(receiver.getName() + " (" + receiver.getUsername() + ") declined your exchange request.");
        n.setActorUserId(receiver.getId());
        n.setActorUsername(receiver.getUsername());
        n.setExchangeRequestId(request.getId());

        Notification saved = notificationRepository.save(n);
        pushToUser(sender.getUsername(), toDto(saved));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForCurrentUser(Pageable pageable) {
        User me = currentUser();
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(me, pageable).map(NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCountForCurrentUser() {
        User me = currentUser();
        return notificationRepository.countByRecipientAndViewedFalse(me);
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        User me = currentUser();
        Notification n = notificationRepository.findByIdAndRecipient(id, me)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!n.isViewed()) {
            n.setViewed(true);
            notificationRepository.save(n);
        }
        return toDto(n);
    }

    @Transactional
    public int markAllReadForCurrentUser() {
        User me = currentUser();
        return notificationRepository.markAllReadForRecipient(me);
    }

    private void pushToUser(String username, NotificationResponse dto) {
        if (username == null || username.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", dto);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
