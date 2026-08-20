package com.carpool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class BookingIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("carpool_test")
        .withUsername("carpool")
        .withPassword("carpool");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> "12345678901234567890123456789012");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void duplicateBookingShould409() throws Exception {
        String mobile = "+919822222222";
        String authReq = "{\"role\":\"PASSENGER\",\"mobile\":\"" + mobile + "\",\"password\":\"passenger123\",\"dateOfBirth\":\"1995-01-01\"}";

        String register = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authReq))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(register).path("data");
        String access = data.path("accessToken").asText();

        String bookingReq = "{\"rideId\":\"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2\",\"seats\":1}";

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingReq))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingReq))
            .andExpect(status().isConflict());
    }

    @Test
    void overbookingShould409() throws Exception {
        String mobile = "+919833333333";
        String authReq = "{\"role\":\"PASSENGER\",\"mobile\":\"" + mobile + "\",\"password\":\"passenger123\",\"dateOfBirth\":\"1995-01-01\"}";

        String register = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authReq))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(register).path("data");
        String access = data.path("accessToken").asText();

        String bookingReq = "{\"rideId\":\"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2\",\"seats\":99}";

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingReq))
            .andExpect(status().isConflict());
    }
}
