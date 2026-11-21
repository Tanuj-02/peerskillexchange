package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
import com.tanuj.peerskillexchange.service.ExchangeRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRequestControllerTest {
    @Test
    void duplicateActiveRequestReturnsConflict() throws Exception {
        ExchangeRequestService service = mock(ExchangeRequestService.class);
        when(service.createExchangeRequest(any(ERRequest.class)))
                .thenThrow(new IllegalStateException("A pending request already exists between these users"));
        var mvc = MockMvcBuilders.standaloneSetup(new ExchangeRequestController(service)).build();

        mvc.perform(post("/api/exchange-requests/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiverId\":\"bob001\",\"message\":\"Swap?\",\"canTeach\":[\"Java\"],\"wantToLearn\":[\"React\"]}"))
                .andExpect(status().isConflict());
    }
}
