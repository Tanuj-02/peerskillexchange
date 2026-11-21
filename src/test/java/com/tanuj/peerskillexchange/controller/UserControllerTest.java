package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.service.UserService;
import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.dto.skill.SkillDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class UserControllerTest {
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void passwordChangeAcceptsJsonBodyAndDoesNotAcceptQueryOnlySecrets() throws Exception {
        UserService service = mock(UserService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));

        mvc.perform(put("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"oldpass99\",\"newPassword\":\"newpass99\",\"verificationCode\":\"123456\"}"))
                .andExpect(status().isOk());
        verify(service).updatePassword("alice", "oldpass99", "newpass99", "123456");

        mvc.perform(put("/api/users/me/password")
                .param("oldPassword", "oldpass99")
                .param("newPassword", "newpass99")
                .param("verificationCode", "123456"))
                .andExpect(status().is4xxClientError());
        verifyNoMoreInteractions(service);
    }

    @Test
    void publicProfileOmitsEmailWhileOwnerProfileIncludesIt() throws Exception {
        UserService service = mock(UserService.class);
        var skills = new SkillDto(java.util.List.of(), java.util.List.of());
        when(service.getById("bob001")).thenReturn(new UserResponse(
                "bob001", "Bob", "bob", null, null, null, null, null, skills));
        when(service.getByUsername("alice")).thenReturn(new UserResponse(
                "alice1", "Alice", "alice", null, null, null,
                "alice@example.test", "USER", skills));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));

        mvc.perform(get("/api/users/bob001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.test"));
    }
}
