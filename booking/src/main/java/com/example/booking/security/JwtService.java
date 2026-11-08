package com.example.booking.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.booking.config.JwtProperties;
import com.example.booking.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.expirationSeconds()))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name());

        if (StringUtils.hasText(properties.issuer())) {
            claims.issuer(properties.issuer());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()));
        return new IssuedToken(jwt.getTokenValue(), properties.expirationSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
