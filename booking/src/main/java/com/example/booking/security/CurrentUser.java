package com.example.booking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.booking.exception.ApiException;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw ApiException.unauthorized("Authentication required");
    }

    public static Long id() {
        Object value = jwt().getClaim("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw ApiException.unauthorized("JWT does not contain userId claim");
    }

    public static String role() {
        return jwt().getClaimAsString("role");
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }
}
