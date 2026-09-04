package com.booking.controller;

import com.booking.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginWithAdminCredentials_shouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
    }

    @Test
    void loginWithUserCredentials_shouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("user123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    void loginWithWrongPassword_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void loginWithNonExistentUser_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankUsername_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
    }

    @Test
    void loginWithoutBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
