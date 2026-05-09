//package com.tanuj.peerskillexchange.mapper;
//
//import com.tanuj.peerskillexchange.dto.exchangeRequest.ERRequest;
//import com.tanuj.peerskillexchange.dto.exchangeRequest.ERResponse;
//import com.tanuj.peerskillexchange.model.ExchangeRequest;
//
//import java.util.List;
//
//public class RequestMapper {
//    public static ERResponse toERRsponse(ExchangeRequest exchangeRequest){
//        return new ERResponse(
//                exchangeRequest.getId(),
//                UserMapper.toUserResponse(exchangeRequest.getSender()),
//                UserMapper.toUserResponse(exchangeRequest.getReceiver()),
//                exchangeRequest.getMessage(),
//                exchangeRequest.getStatus().name(),
//                exchangeRequest.getOfferedSkills().get(0).getSkillname(),
//                exchangeRequest.getAskingSkills().get(0).getSkillname(),
//                exchangeRequest.getCreatedAt()
//        );
//    }
//
//    public static ExchangeRequest toExchangeRequest(ERRequest erRequest){
//
//        ExchangeRequest exchangeRequest = new ExchangeRequest();
//        exchangeRequest.setSender(erRequest.sender());
//        exchangeRequest.setReceiver(erRequest.receiver());
//        exchangeRequest.setMessage(erRequest.message());
////        exchangeRequest.setOfferedSkills(offeredSkills);
//    }
//}
