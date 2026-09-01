package com.booking.controller;

import com.booking.dto.request.LoginRequest;
import com.booking.dto.request.ReservationRequest;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getToken("admin", "admin123");
        user1Token = getToken("user1", "user123");
        user2Token = getToken("user2", "user123");
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
    void getReservationsWithoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReservation_asUser_shouldUseJwtIdentity() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setNotes("Test booking");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("user1")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.totalPrice", notNullValue()));
    }

    @Test
    void getReservations_asUser_shouldOnlySeeOwnReservations() throws Exception {
        // user1 creates a reservation
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // user2 creates their own reservation
        ReservationRequest request2 = new ReservationRequest();
        request2.setResourceId(1L);
        request2.setStartTime(LocalDateTime.now().plusDays(3));
        request2.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // user1 listing should only see their own
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", everyItem(is("user1"))));
    }

    @Test
    void getReservations_asAdmin_shouldSeeAllReservations() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void getReservation_byUser2_owningUser1Reservation_shouldReturn403() throws Exception {
        // user1 creates a reservation
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(4));
        request.setEndTime(LocalDateTime.now().plusDays(4).plusHours(1));

        MvcResult createResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reservationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        // user2 tries to access user1's reservation — should be forbidden
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReservationStatus_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/reservations/1/status")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReservationStatus_asAdmin_shouldSucceed() throws Exception {
        // Create a reservation as user1
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(5));
        request.setEndTime(LocalDateTime.now().plusDays(5).plusHours(2));

        MvcResult createResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reservationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        // Admin confirms the reservation
        mockMvc.perform(patch("/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void createReservation_withEndTimeBeforeStartTime_shouldReturn400() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1)); // end before start

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterReservations_byStatus_shouldWork() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))));
    }

    @Test
    void deleteReservationByOtherUser_shouldReturn403() throws Exception {
        // user1 creates a reservation
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(6));
        request.setEndTime(LocalDateTime.now().plusDays(6).plusHours(1));

        MvcResult createResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reservationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        // user2 tries to delete user1's reservation
        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}
