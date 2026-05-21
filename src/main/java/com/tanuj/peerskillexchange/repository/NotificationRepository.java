package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.Notification;
import com.tanuj.peerskillexchange.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndViewedFalse(User recipient);

    Optional<Notification> findByIdAndRecipient(Long id, User recipient);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.viewed = true where n.recipient = :recipient and n.viewed = false")
    int markAllReadForRecipient(@Param("recipient") User recipient);
}
