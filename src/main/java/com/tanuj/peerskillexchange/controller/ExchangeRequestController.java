package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
import com.tanuj.peerskillexchange.dto.exchangeRequest.ERResponse;
import com.tanuj.peerskillexchange.service.ExchangeRequestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-requests")
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;

    public ExchangeRequestController(ExchangeRequestService exchangeRequestService) {
        this.exchangeRequestService = exchangeRequestService;
    }

    @PostMapping("/create")
    public ERResponse createExchangeRequest(@RequestBody ERRequest erRequest) {
        return exchangeRequestService.createExchangeRequest(erRequest);
    }
}
