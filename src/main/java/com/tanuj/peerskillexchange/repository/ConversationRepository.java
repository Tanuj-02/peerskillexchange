package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.Conversation;
import com.tanuj.peerskillexchange.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1AndUser2(User user1, User user2);

    @Query("""
            select c
            from Conversation c
            where c.user1 = :me or c.user2 = :me
            """)
    Page<Conversation> findForUser(@Param("me") User me, Pageable pageable);
}

