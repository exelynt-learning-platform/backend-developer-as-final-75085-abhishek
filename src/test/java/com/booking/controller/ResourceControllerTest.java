package com.booking.controller;

import com.booking.dto.request.LoginRequest;
import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getToken("admin", "admin123");
        userToken = getToken("user1", "user123");
    }

    private String getToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.getToken();
    }

    @Test
    void getAllResources_asAuthenticatedUser_shouldSucceed() throws Exception {
        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void getAllResources_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResourceById_asUser_shouldSucceed() throws Exception {
        mockMvc.perform(get("/resources/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void getResourceById_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(get("/resources/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void createResource_asAdmin_shouldReturn201() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("Test Room");
        request.setDescription("A test room");
        request.setType("ROOM");
        request.setCapacity(10);
        request.setPricePerHour(new BigDecimal("30.00"));
        request.setAvailable(true);

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Room")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void createResource_asUser_shouldReturn403() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("Unauthorized Room");
        request.setType("ROOM");
        request.setPricePerHour(new BigDecimal("10.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_withMissingRequiredFields_shouldReturn400() throws Exception {
        ResourceRequest request = new ResourceRequest();

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(greaterThan(0))));
    }

    @Test
    void updateResource_asAdmin_shouldSucceed() throws Exception {
        ResourceRequest createRequest = new ResourceRequest();
        createRequest.setName("Room To Update");
        createRequest.setType("ROOM");
        createRequest.setPricePerHour(new BigDecimal("20.00"));

        MvcResult createResult = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        ResourceRequest updateRequest = new ResourceRequest();
        updateRequest.setName("Updated Room Name");
        updateRequest.setType("ROOM");
        updateRequest.setPricePerHour(new BigDecimal("25.00"));
        updateRequest.setAvailable(false);

        mockMvc.perform(put("/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Room Name")));
    }

    @Test
    void deleteResource_asAdmin_shouldReturn204() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("Room To Delete");
        request.setType("ROOM");
        request.setPricePerHour(new BigDecimal("10.00"));

        MvcResult createResult = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteResource_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/resources/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllResources_paginationAndSorting_shouldWork() throws Exception {
        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize", is(2)))
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))));
    }
}
