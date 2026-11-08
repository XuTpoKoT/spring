package com.example.booking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.exception.ApiException;
import com.example.booking.model.AuthRequestDto;
import com.example.booking.model.AuthResponseDto;
import com.example.booking.model.RegisterRequestDto;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import com.example.booking.security.JwtService.IssuedToken;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String username = request.getUsername().trim();
        if (users.existsByUsername(username)) {
            throw ApiException.conflict("User with username '" + username + "' already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        users.save(user);

        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto authenticate(AuthRequestDto request) {
        String username = request.getUsername().trim();
        User user = users.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid username or password");
        }

        return tokenResponse(user);
    }

    private AuthResponseDto tokenResponse(User user) {
        IssuedToken token = jwtService.issue(user);
        return new AuthResponseDto(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                user.getId(),
                user.getUsername(),
                AuthResponseDto.RoleEnum.valueOf(user.getRole().name())
        );
    }
}
