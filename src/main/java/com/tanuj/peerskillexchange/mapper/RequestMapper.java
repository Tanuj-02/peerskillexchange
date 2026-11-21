package com.tanuj.peerskillexchange.mapper;

import com.tanuj.peerskillexchange.dto.exchangeRequest.ERResponse;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.Skill;

import java.util.Collections;
import java.util.List;

public class RequestMapper {
    public static ERResponse toERResponse(ExchangeRequest exchangeRequest) {
        return new ERResponse(
                exchangeRequest.getId(),
                UserMapper.toPublicUserResponse(exchangeRequest.getSender()),
                UserMapper.toPublicUserResponse(exchangeRequest.getReceiver()),
                exchangeRequest.getMessage(),
                exchangeRequest.getStatus().name(),
                skillNames(exchangeRequest.getOfferedSkills()),
                skillNames(exchangeRequest.getAskingSkills()),
                exchangeRequest.getCreatedAt()
        );
    }

    private static List<String> skillNames(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }
        return skills.stream().map(Skill::getSkillname).toList();
    }
}
