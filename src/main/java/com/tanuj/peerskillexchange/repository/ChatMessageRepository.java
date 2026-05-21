package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.ChatMessage;
import com.tanuj.peerskillexchange.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m
            from ChatMessage m
            where m.conversation = :conversation
            order by m.createdAt desc
            """)
    Page<ChatMessage> findByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

    void deleteByConversation(Conversation conversation);
}

