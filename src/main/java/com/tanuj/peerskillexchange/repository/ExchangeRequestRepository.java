package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select er from ExchangeRequest er where er.id = :id")
    java.util.Optional<ExchangeRequest> lockById(@Param("id") Long id);
    List<ExchangeRequest> findBySenderOrderByCreatedAtDesc(User sender);
    List<ExchangeRequest> findByReceiverOrderByCreatedAtDesc(User receiver);

    @Query("""
            select count(er) > 0
            from ExchangeRequest er
            where er.status = :status
              and (
                   (er.sender = :u1 and er.receiver = :u2)
                or (er.sender = :u2 and er.receiver = :u1)
              )
            """)
    boolean existsConnectionBetween(
            @Param("u1") User u1,
            @Param("u2") User u2,
            @Param("status") RequestStatus status
    );

    @Query("""
            select er
            from ExchangeRequest er
            where er.status = :status
              and (
                   (er.sender = :u1 and er.receiver = :u2)
                or (er.sender = :u2 and er.receiver = :u1)
              )
            """)
    List<ExchangeRequest> findConnectionsBetween(
            @Param("u1") User u1,
            @Param("u2") User u2,
            @Param("status") RequestStatus status
    );
}
