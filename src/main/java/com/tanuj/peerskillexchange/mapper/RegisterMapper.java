package com.tanuj.peerskillexchange.mapper;

import com.tanuj.peerskillexchange.dto.auth.RegisterRequest;
import com.tanuj.peerskillexchange.dto.auth.RegisterResponse;
import com.tanuj.peerskillexchange.model.User;

public class RegisterMapper {
    public static RegisterResponse toRegisterResponse(User user){
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getBio(),
                user.getProfilePicture(),
                user.getCollege(),
                user.getRole().name()
        );
    }

    public static User toUserEntity(RegisterRequest registerRequest){
        User user = new User();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setPassword(registerRequest.password());
        user.setName(registerRequest.name());
        user.setBio(registerRequest.bio());
        user.setProfilePicture(registerRequest.ProfilePicture());
        user.setCollege(registerRequest.college());
        return user;
    }
}
