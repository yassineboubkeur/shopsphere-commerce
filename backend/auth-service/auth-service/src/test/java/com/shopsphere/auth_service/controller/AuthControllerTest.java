package com.shopsphere.auth_service.controller;

import com.shopsphere.auth_service.dto.AuthResponse;
import com.shopsphere.auth_service.dto.LoginRequest;
import com.shopsphere.auth_service.dto.RegisterRequest;
import com.shopsphere.auth_service.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("POST /register - returns 201 Created with the auth response")
    void register_returnsCreated() {
        AuthResponse expected = AuthResponse.builder()
                .token("jwt")
                .type("Bearer")
                .id(1L)
                .username("yassine")
                .email("yassine@example.com")
                .role("USER")
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(expected);

        RegisterRequest request = RegisterRequest.builder()
                .username("yassine")
                .email("yassine@example.com")
                .password("Str0ng!Passw0rd")
                .build();

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(response.getBody().getToken()).isEqualTo("jwt");
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /login - returns 200 OK with the auth response")
    void login_returnsOk() {
        AuthResponse expected = AuthResponse.builder()
                .token("jwt")
                .type("Bearer")
                .id(2L)
                .username("boss")
                .email("boss@example.com")
                .role("ADMIN")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(expected);

        LoginRequest request = LoginRequest.builder()
                .email("boss@example.com")
                .password("Str0ng!Passw0rd")
                .build();

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        verify(authService).login(any(LoginRequest.class));
    }
}
