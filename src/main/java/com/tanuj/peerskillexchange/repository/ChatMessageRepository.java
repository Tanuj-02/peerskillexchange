package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.ChatMessage;
import com.tanuj.peerskillexchange.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m
            from ChatMessage m
            where m.conversation = :conversation
            order by m.createdAt desc
            """)
    Page<ChatMessage> findByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

    @Query("select m from ChatMessage m where m.conversation = :conversation and m.createdAt between :start and :end order by m.createdAt desc")
    Page<ChatMessage> findForRoom(@Param("conversation") Conversation conversation,
                                  @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                  Pageable pageable);

    void deleteByConversation(Conversation conversation);
}

