package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
}
