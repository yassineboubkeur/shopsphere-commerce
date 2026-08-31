package com.shopsphere.auth_service.service;

import com.shopsphere.auth_service.dto.AuthResponse;
import com.shopsphere.auth_service.dto.LoginRequest;
import com.shopsphere.auth_service.dto.RegisterRequest;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        String usernameLower = request.getUsername().toLowerCase().trim();
        String emailLocalPart = request.getEmail().toLowerCase().split("@")[0].trim();
        String password = request.getPassword().toLowerCase();
        if (password.contains(usernameLower) || (password.contains(emailLocalPart) && emailLocalPart.length() > 2)) {
            throw new RuntimeException("Password must not contain your username or email.");
        }

        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        String role = user.getRoles().iterator().next().getName().name();
        String token = jwtUtil.generateToken(user.getEmail(), role);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String role = user.getRoles().iterator().next().getName().name();
        String token = jwtUtil.generateToken(user.getEmail(), role);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role)
                .build();
    }
}
