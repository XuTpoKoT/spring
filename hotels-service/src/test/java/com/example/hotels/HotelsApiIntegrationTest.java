package com.example.hotels;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelsApiIntegrationTest {

    private static final String JWT_SECRET = "test-secret-test-secret-test-secret-123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminCanCreateHotel() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Hotel-" + UUID.randomUUID(), "address", "Address"));

        mockMvc.perform(post("/admin/hotels")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/hotels")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void confirmsAndReleasesAvailabilityIdempotently() throws Exception {
        String admin = "Bearer " + token("ADMIN");
        String user = "Bearer " + token("USER");

        MvcResult hotelResult = mockMvc.perform(post("/admin/hotels")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Hotel-" + UUID.randomUUID(), "address", "Address"))))
                .andExpect(status().isCreated())
                .andReturn();
        long hotelId = objectMapper.readTree(hotelResult.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult roomResult = mockMvc.perform(post("/admin/rooms")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("hotelId", hotelId, "number", "R" + UUID.randomUUID().toString().substring(0, 8)))))
                .andExpect(status().isCreated())
                .andReturn();
        long roomId = objectMapper.readTree(roomResult.getResponse().getContentAsString())
                .get("id").asLong();

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        String availabilityBody = objectMapper.writeValueAsString(Map.of(
                "bookingId", 42,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()));

        mockMvc.perform(post("/rooms/{id}/confirm-availability", roomId)
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .header("X-Request-Id", "it-confirm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));

        mockMvc.perform(post("/rooms/{id}/confirm-availability", roomId)
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .header("X-Request-Id", "it-confirm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));

        mockMvc.perform(post("/rooms/{id}/release", roomId)
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .header("X-Request-Id", "it-release-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bookingId", 42))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/rooms/{id}/confirm-availability", roomId)
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .header("X-Request-Id", "it-confirm-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityBody))
                .andExpect(status().isOk());
    }

    private String token(String role) {
        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("tester")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", 1L)
                .claim("role", role)
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
