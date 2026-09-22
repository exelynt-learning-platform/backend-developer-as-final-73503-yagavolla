package com.example.booking.config;

import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@Profile("dev")
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(UserRepository users, ResourceRepository resources, PasswordEncoder encoder,
                               @Value("${app.seed.admin-password}") String adminPassword,
                               @Value("${app.seed.user-password}") String userPassword) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                users.save(new User("admin", "admin@example.com", encoder.encode(adminPassword), Role.ADMIN));
            }
            if (users.findByUsername("user").isEmpty()) {
                users.save(new User("user", "user@example.com", encoder.encode(userPassword), Role.USER));
            }
            if (resources.count() == 0) {
                resources.save(new Resource("Meeting Room A", "Ten-person meeting room", "ROOM", new BigDecimal("50.00"), true));
                resources.save(new Resource("Company Van", "Seven-seat vehicle", "VEHICLE", new BigDecimal("80.00"), true));
            }
        };
    }
}