package com.booking.seed;

import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.enums.Role;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@booking.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .build());
            log.info("Seed: Created admin user (username=admin, password=admin123)");
        }

        if (!userRepository.existsByUsername("user1")) {
            userRepository.save(User.builder()
                    .username("user1")
                    .email("user1@booking.com")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.ROLE_USER)
                    .build());
            log.info("Seed: Created user1 (username=user1, password=user123)");
        }

        if (!userRepository.existsByUsername("user2")) {
            userRepository.save(User.builder()
                    .username("user2")
                    .email("user2@booking.com")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.ROLE_USER)
                    .build());
            log.info("Seed: Created user2 (username=user2, password=user123)");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            List<Resource> resources = List.of(
                    Resource.builder()
                            .name("Conference Room A")
                            .description("A spacious conference room with projector and whiteboard, capacity 20 people.")
                            .type("ROOM")
                            .capacity(20)
                            .pricePerHour(new BigDecimal("50.00"))
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Toyota Camry - Fleet Vehicle 1")
                            .description("Company fleet vehicle available for business trips.")
                            .type("VEHICLE")
                            .capacity(5)
                            .pricePerHour(new BigDecimal("25.00"))
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Professional Camera Kit")
                            .description("Sony A7 IV camera kit with lenses and lighting equipment.")
                            .type("EQUIPMENT")
                            .capacity(1)
                            .pricePerHour(new BigDecimal("15.00"))
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Training Room B")
                            .description("Training room with 30 workstations, ideal for workshops.")
                            .type("ROOM")
                            .capacity(30)
                            .pricePerHour(new BigDecimal("75.00"))
                            .available(true)
                            .build(),
                    Resource.builder()
                            .name("Projector - Portable 4K")
                            .description("Portable 4K projector for presentations and events.")
                            .type("EQUIPMENT")
                            .capacity(1)
                            .pricePerHour(new BigDecimal("10.00"))
                            .available(true)
                            .build()
            );
            resourceRepository.saveAll(resources);
            log.info("Seed: Created {} sample resources", resources.size());
        }
    }
}
