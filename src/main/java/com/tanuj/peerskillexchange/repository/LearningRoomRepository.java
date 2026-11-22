package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.LearningRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface LearningRoomRepository extends JpaRepository<LearningRoom, Long> {
    Optional<LearningRoom> findByExchangeRequestId(Long exchangeRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LearningRoom r where r.id = :id")
    Optional<LearningRoom> lockById(@Param("id") Long id);

    @Query("select r from LearningRoom r where r.owner.id = :userId or r.peer.id = :userId order by r.createdAt desc")
    List<LearningRoom> findForUser(@Param("userId") String userId);
}
