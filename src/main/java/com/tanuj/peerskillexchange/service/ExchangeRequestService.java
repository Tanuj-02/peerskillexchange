package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
import com.tanuj.peerskillexchange.dto.exchangeRequest.ERResponse;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.mapper.RequestMapper;
import com.tanuj.peerskillexchange.model.Conversation;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.Skill;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ChatMessageRepository;
import com.tanuj.peerskillexchange.repository.ConversationRepository;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.SkillRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;

    public ExchangeRequestService(
            ExchangeRequestRepository exchangeRequestRepository,
            UserRepository userRepository,
            SkillRepository skillRepository,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            NotificationService notificationService
    ) {
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ERResponse createExchangeRequest(ERRequest erRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        User receiver = userRepository.findById(erRequest.receiverId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("You cannot send an exchange request to yourself");
        }

        // Lock both users in a stable order so simultaneous requests in either direction
        // cannot both pass the active-request checks.
        String first = sender.getId().compareTo(receiver.getId()) < 0 ? sender.getId() : receiver.getId();
        String second = sender.getId().compareTo(receiver.getId()) < 0 ? receiver.getId() : sender.getId();
        userRepository.lockById(first).orElseThrow(() -> new IllegalArgumentException("User not found."));
        userRepository.lockById(second).orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (exchangeRequestRepository.existsConnectionBetween(sender, receiver, RequestStatus.ACCEPTED)) {
            throw new IllegalStateException("You are already connected with this user");
        }

        if (exchangeRequestRepository.existsConnectionBetween(sender, receiver, RequestStatus.PENDING)) {
            throw new IllegalStateException("A pending request already exists between these users");
        }

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setSender(sender);
        exchangeRequest.setReceiver(receiver);
        exchangeRequest.setMessage(erRequest.message());
        exchangeRequest.setStatus(RequestStatus.PENDING);

        exchangeRequest.setOfferedSkills(resolveSkills(erRequest.canTeach()));
        exchangeRequest.setAskingSkills(resolveSkills(erRequest.wantToLearn()));

        ExchangeRequest saved = exchangeRequestRepository.save(exchangeRequest);
        notificationService.notifyExchangeRequestReceived(saved);
        return RequestMapper.toERResponse(saved);
    }

    public List<ERResponse> getMySentRequests() {
        User me = getCurrentUser();
        return exchangeRequestRepository.findBySenderOrderByCreatedAtDesc(me)
                .stream()
                .map(RequestMapper::toERResponse)
                .toList();
    }

    public List<ERResponse> getMyReceivedRequests() {
        User me = getCurrentUser();
        return exchangeRequestRepository.findByReceiverOrderByCreatedAtDesc(me)
                .stream()
                .map(RequestMapper::toERResponse)
                .toList();
    }

    public ERResponse acceptRequest(Long requestId) {
        ExchangeRequest req = getRequestForReceiver(requestId);
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be accepted");
        }
        req.setStatus(RequestStatus.ACCEPTED);
        ExchangeRequest updated = exchangeRequestRepository.save(req);
        ensureConversationExists(updated.getSender(), updated.getReceiver());
        notificationService.notifyExchangeAccepted(updated);
        return RequestMapper.toERResponse(updated);
    }

    public ERResponse rejectRequest(Long requestId) {
        ExchangeRequest req = getRequestForReceiver(requestId);
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be rejected");
        }
        req.setStatus(RequestStatus.REJECTED);
        ExchangeRequest updated = exchangeRequestRepository.save(req);
        notificationService.notifyExchangeRejected(updated);
        return RequestMapper.toERResponse(updated);
    }

    private static boolean isBefore(User a, User b) {
        return a.getId().compareTo(b.getId()) < 0;
    }

    private void ensureConversationExists(User a, User b) {
        User u1 = isBefore(a, b) ? a : b;
        User u2 = isBefore(a, b) ? b : a;
        conversationRepository.findByUser1AndUser2(u1, u2).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setUser1(u1);
            c.setUser2(u2);
            return conversationRepository.save(c);
        });
    }

    @Transactional
    public String disconnect(String otherUserId) {
        User me = getCurrentUser();
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (me.getId().equals(other.getId())) {
            throw new IllegalArgumentException("Cannot disconnect from yourself.");
        }

        List<ExchangeRequest> connections = exchangeRequestRepository.findConnectionsBetween(me, other, RequestStatus.ACCEPTED);
        if (connections.isEmpty()) {
            throw new IllegalArgumentException("Users are not connected.");
        }


        exchangeRequestRepository.deleteAll(connections);

        User u1 = isBefore(me, other) ? me : other;
        User u2 = isBefore(me, other) ? other : me;
        Optional<Conversation> conv = conversationRepository.findByUser1AndUser2(u1, u2);
        conv.ifPresent(c -> {
            chatMessageRepository.deleteByConversation(c);
            conversationRepository.delete(c);
        });

        return "Disconnected successfully";
    }

    private ExchangeRequest getRequestForReceiver(Long requestId) {
        User me = getCurrentUser();
        ExchangeRequest req = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Exchange request not found"));

        if (req.getReceiver() == null || req.getReceiver().getId() == null || !req.getReceiver().getId().equals(me.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        return req;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private List<Skill> resolveSkills(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Skill> skills = new ArrayList<>();
        for (String raw : skillNames) {
            if (raw == null) continue;
            String skillName = raw.trim().toLowerCase();
            if (skillName.isEmpty()) continue;

            Skill skill = skillRepository.findBySkillnameIgnoreCase(skillName).orElseGet(() -> {
                Skill created = new Skill();
                created.setSkillname(skillName);
                return skillRepository.save(created);
            });
            skills.add(skill);
        }
        return skills;
    }
}
