package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.model.*;
import com.tanuj.peerskillexchange.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Explicit test-scope seed utility; it is never packaged with the application. */
final class DemoDataSeeder {
    static final String DEMO_PASSWORD = "Pass@123";
    private static final String COLLEGE = "Demo Institute of Technology";
    private static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("D00001", "demo_aarav", "Aarav Sharma", "Frontend developer exploring backend systems.",
                    List.of("react", "typescript"), List.of("spring boot", "java", "docker")),
            new DemoUser("D00002", "demo_meera", "Meera Iyer", "Java developer who enjoys building reliable APIs.",
                    List.of("spring boot", "java"), List.of("react", "css", "python")),
            new DemoUser("D00003", "demo_isha", "Isha Patel", "Product designer interested in deployment workflows.",
                    List.of("figma", "ui design"), List.of("docker", "aws", "python")),
            new DemoUser("D00004", "demo_kabir", "Kabir Khan", "Cloud engineer learning design and frontend work.",
                    List.of("docker", "aws"), List.of("figma", "ui design", "react")),
            new DemoUser("D00005", "demo_riya", "Riya Sen", "Machine learning student learning data systems.",
                    List.of("python", "machine learning"), List.of("postgresql", "sql", "spring boot", "figma")),
            new DemoUser("D00006", "demo_dev", "Dev Nair", "Database enthusiast exploring machine learning.",
                    List.of("postgresql", "sql"), List.of("python", "machine learning"))
    );

    private final UserRepository users;
    private final SkillRepository skills;
    private final UserSkillRepository userSkills;
    private final ExchangeRequestRepository requests;
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final LearningRoomRepository rooms;
    private final BCryptPasswordEncoder encoder;
    private final TransactionTemplate transactions;

    DemoDataSeeder(UserRepository users, SkillRepository skills, UserSkillRepository userSkills,
                   ExchangeRequestRepository requests, ConversationRepository conversations,
                   ChatMessageRepository messages, LearningRoomRepository rooms,
                   BCryptPasswordEncoder encoder, TransactionTemplate transactions) {
        this.users = users;
        this.skills = skills;
        this.userSkills = userSkills;
        this.requests = requests;
        this.conversations = conversations;
        this.messages = messages;
        this.rooms = rooms;
        this.encoder = encoder;
        this.transactions = transactions;
    }

    record SeedResult(boolean inserted, int users, int skills, int requests, int rooms, int messages) {}

    SeedResult seed() {
        return transactions.execute(status -> {
            long existing = DEMO_USERS.stream().filter(spec -> users.findByUsername(spec.username()).isPresent()).count();
            if (existing == DEMO_USERS.size()) {
                for (DemoUser spec : DEMO_USERS) {
                    User user = users.findByUsername(spec.username()).orElseThrow();
                    if (!spec.id().equals(user.getId()) || !email(spec).equals(user.getEmail())
                            || !encoder.matches(DEMO_PASSWORD, user.getPassword())) {
                        throw new IllegalStateException("Existing demo account differs: " + spec.username());
                    }
                }
                return new SeedResult(false, 0, 0, 0, 0, 0);
            }
            if (existing != 0) throw new IllegalStateException("Partial demo data exists; no records were changed.");
            for (DemoUser spec : DEMO_USERS) {
                if (users.existsById(spec.id()) || users.findByEmail(email(spec)).isPresent()) {
                    throw new IllegalStateException("Demo ID or email is already in use: " + spec.username());
                }
            }

            Map<String, User> created = DEMO_USERS.stream().map(spec -> {
                User user = new User();
                user.setId(spec.id());
                user.setUsername(spec.username());
                user.setEmail(email(spec));
                user.setName(spec.name());
                user.setPassword(encoder.encode(DEMO_PASSWORD));
                user.setEmailVerified(true);
                user.setRole(Role.USER);
                user.setCollege(COLLEGE);
                user.setBio(spec.bio());
                return users.saveAndFlush(user);
            }).collect(Collectors.toMap(User::getUsername, user -> user));

            int oldSkillCount = (int) skills.count();
            for (DemoUser spec : DEMO_USERS) {
                for (String name : spec.teaches()) addUserSkill(created.get(spec.username()), name, SkillType.TEACH);
                for (String name : spec.learns()) addUserSkill(created.get(spec.username()), name, SkillType.LEARN);
            }

            accepted(created, "demo_aarav", "demo_meera", "I can help with React while learning Spring Boot.",
                    "react", "spring boot", "Let us compare React components and Spring controllers.",
                    "Sounds good. We can sketch the API on our whiteboard.");
            accepted(created, "demo_isha", "demo_kabir", "I can teach Figma if you show me Docker.",
                    "figma", "docker", "I made a wireframe for our practice session.",
                    "Great. I will show you how to containerize it.");
            accepted(created, "demo_riya", "demo_dev", "I can explain Python in exchange for PostgreSQL help.",
                    "python", "postgresql", "Let us start with a small data model.",
                    "I can show indexes and then learn your Python workflow.");
            request(created, "demo_aarav", "demo_kabir", RequestStatus.PENDING,
                    "Could we swap React for Docker basics?", "react", "docker");
            request(created, "demo_meera", "demo_riya", RequestStatus.PENDING,
                    "I would like to learn Python and share Spring Boot.", "spring boot", "python");
            request(created, "demo_isha", "demo_riya", RequestStatus.REJECTED,
                    "An earlier Figma and Python exchange idea.", "figma", "python");
            return new SeedResult(true, DEMO_USERS.size(), (int) skills.count() - oldSkillCount,
                    6, 3, 6);
        });
    }

    private static String email(DemoUser spec) { return spec.username() + "@example.test"; }

    private void addUserSkill(User user, String name, SkillType type) {
        Skill skill = skills.findBySkillnameIgnoreCase(name).orElseGet(() -> {
            Skill created = new Skill();
            created.setSkillname(name);
            return skills.saveAndFlush(created);
        });
        UserSkill link = new UserSkill();
        link.setUsers(user);
        link.setSkill(skill);
        link.setType(type);
        userSkills.save(link);
    }

    private ExchangeRequest request(Map<String, User> created, String from, String to,
                                    RequestStatus status, String text, String offered, String asking) {
        ExchangeRequest request = new ExchangeRequest();
        request.setSender(created.get(from));
        request.setReceiver(created.get(to));
        request.setStatus(status);
        request.setMessage(text);
        request.setOfferedSkills(new ArrayList<>(List.of(skills.findBySkillnameIgnoreCase(offered).orElseThrow())));
        request.setAskingSkills(new ArrayList<>(List.of(skills.findBySkillnameIgnoreCase(asking).orElseThrow())));
        return requests.saveAndFlush(request);
    }

    private void accepted(Map<String, User> created, String from, String to, String text,
                          String offered, String asking, String firstMessage, String secondMessage) {
        ExchangeRequest accepted = request(created, from, to, RequestStatus.ACCEPTED, text, offered, asking);
        User sender = created.get(from);
        User receiver = created.get(to);
        Conversation conversation = new Conversation();
        conversation.setUser1(sender.getId().compareTo(receiver.getId()) < 0 ? sender : receiver);
        conversation.setUser2(sender.getId().compareTo(receiver.getId()) < 0 ? receiver : sender);
        conversation = conversations.saveAndFlush(conversation);
        message(conversation, sender, firstMessage);
        message(conversation, receiver, secondMessage);
        LearningRoom room = new LearningRoom();
        room.setExchangeRequestId(accepted.getId());
        room.setOwner(sender);
        room.setPeer(receiver);
        rooms.save(room);
    }

    private void message(Conversation conversation, User sender, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        messages.save(message);
    }

    private record DemoUser(String id, String username, String name, String bio,
                            List<String> teaches, List<String> learns) {}
}
