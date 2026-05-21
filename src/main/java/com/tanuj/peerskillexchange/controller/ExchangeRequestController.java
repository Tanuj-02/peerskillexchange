package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
import com.tanuj.peerskillexchange.dto.exchangeRequest.ERResponse;
import com.tanuj.peerskillexchange.service.ExchangeRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchange-requests")
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;

    public ExchangeRequestController(ExchangeRequestService exchangeRequestService) {
        this.exchangeRequestService = exchangeRequestService;
    }

    @PostMapping("/create")
    public ERResponse createExchangeRequest(@Valid @RequestBody ERRequest erRequest) {
        return exchangeRequestService.createExchangeRequest(erRequest);
    }

    @GetMapping("/me/sent")
    public List<ERResponse> mySentRequests() {
        return exchangeRequestService.getMySentRequests();
    }

    @GetMapping("/me/received")
    public List<ERResponse> myReceivedRequests() {
        return exchangeRequestService.getMyReceivedRequests();
    }

    @PutMapping("/{id}/accept")
    public ERResponse accept(@PathVariable("id") Long id) {
        return exchangeRequestService.acceptRequest(id);
    }

    @PutMapping("/{id}/reject")
    public ERResponse reject(@PathVariable("id") Long id) {
        return exchangeRequestService.rejectRequest(id);
    }

    @DeleteMapping("/disconnect/{otherUserId}")
    public String disconnect(@PathVariable String otherUserId) {
        return exchangeRequestService.disconnect(otherUserId);
    }
}
