package com.tanuj.peerskillexchange.dto.exchangeRequest;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.model.User;

public record ERRequest(
        User sender,
        User receiver,
        String message,
        String canTeach,
        String wantToLearn
) {
}
