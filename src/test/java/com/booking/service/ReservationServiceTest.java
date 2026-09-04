package com.booking.service;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.ReservationStatusRequest;
import com.booking.dto.response.ReservationResponse;
import com.booking.entity.Reservation;
import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.enums.ReservationStatus;
import com.booking.enums.Role;
import com.booking.exception.ResourceNotFoundException;
import com.booking.exception.UnauthorizedException;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User user1;
    private User user2;
    private User adminUser;
    private Resource resource;
    private Reservation reservation;

    private UserDetails user1Details;
    private UserDetails user2Details;
    private UserDetails adminDetails;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).username("user1").role(Role.ROLE_USER).build();
        user2 = User.builder().id(2L).username("user2").role(Role.ROLE_USER).build();
        adminUser = User.builder().id(3L).username("admin").role(Role.ROLE_ADMIN).build();

        resource = Resource.builder()
                .id(1L).name("Conference Room A").type("ROOM")
                .pricePerHour(new BigDecimal("50.00")).available(true).build();

        reservation = Reservation.builder()
                .id(1L).user(user1).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(ReservationStatus.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .build();

        user1Details = new org.springframework.security.core.userdetails.User(
                "user1", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        user2Details = new org.springframework.security.core.userdetails.User(
                "user2", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        adminDetails = new org.springframework.security.core.userdetails.User(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void createReservation_shouldUseJwtUsernameNotRequestBody() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        ReservationResponse response = reservationService.createReservation(request, user1Details);

        assertThat(response.getUsername()).isEqualTo("user1");
        verify(userRepository).findByUsername("user1"); // user from JWT, not request
    }

    @Test
    void createReservation_withUnavailableResource_shouldThrow() {
        resource.setAvailable(false);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> reservationService.createReservation(request, user1Details))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createReservation_withInvalidTimeRange_shouldThrow() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1)); // end before start

        // No stubs needed — validation fires before any repository call
        assertThatThrownBy(() -> reservationService.createReservation(request, user1Details))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void getReservationById_asOwner_shouldSucceed() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(1L, user1Details);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getReservationById_asNonOwner_shouldThrowUnauthorized() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(1L, user2Details))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getReservationById_asAdmin_canAccessAnyReservation() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(1L, adminDetails);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void updateReservationStatus_shouldChangeStatus() {
        ReservationStatusRequest statusRequest = new ReservationStatusRequest();
        statusRequest.setStatus(ReservationStatus.CONFIRMED);

        Reservation confirmed = Reservation.builder()
                .id(1L).user(user1).resource(resource)
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(new BigDecimal("100.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(confirmed);

        ReservationResponse response = reservationService.updateReservationStatus(1L, statusRequest);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void deleteReservation_asNonOwner_shouldThrowUnauthorized() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.deleteReservation(1L, user2Details))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deleteReservation_asOwner_shouldSucceed() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(1L, user1Details);

        verify(reservationRepository).delete(reservation);
    }

    @Test
    void deleteReservation_asAdmin_shouldSucceedForAnyReservation() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(1L, adminDetails);

        verify(reservationRepository).delete(reservation);
    }
}
