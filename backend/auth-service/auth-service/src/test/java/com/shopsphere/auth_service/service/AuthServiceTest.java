package com.shopsphere.auth_service.service;

import com.shopsphere.auth_service.dto.AuthResponse;
import com.shopsphere.auth_service.dto.LoginRequest;
import com.shopsphere.auth_service.dto.RegisterRequest;
import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1L).name(Role.RoleName.USER).build();
        user = User.builder()
                .id(10L)
                .username("yassine")
                .email("yassine@example.com")
                .password("encoded-password")
                .roles(Collections.singleton(userRole))
                .build();

        registerRequest = RegisterRequest.builder()
                .username("yassine")
                .email("Yassine@example.com")
                .password("Str0ng!Passw0rd")
                .build();
    }

    @Test
    @DisplayName("register - success returns Bearer token and user details")
    void register_success() {
        when(userService.register(eq("yassine"), eq("Yassine@example.com"), eq("Str0ng!Passw0rd")))
                .thenReturn(user);
        when(jwtUtil.generateToken("yassine@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUsername()).isEqualTo("yassine");
        assertThat(response.getEmail()).isEqualTo("yassine@example.com");
        assertThat(response.getRole()).isEqualTo("USER");

        verify(userService).register(eq("yassine"), eq("Yassine@example.com"), eq("Str0ng!Passw0rd"));
        verify(jwtUtil).generateToken("yassine@example.com", "USER");
    }

    @Test
    @DisplayName("register - rejects when password contains the username")
    void register_rejectsPasswordContainingUsername() {
        registerRequest.setPassword("yassine123");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Password must not contain your username or email.");

        verify(userService, never()).register(any(), any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("register - rejects when password contains the email local part")
    void register_rejectsPasswordContainingEmailLocalPart() {
        registerRequest.setPassword("Yassine!99");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Password must not contain your username or email.");

        verify(userService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("login - success returns token for valid credentials")
    void login_success() {
        when(userService.findByEmail("yassine@example.com")).thenReturn(user);
        when(passwordEncoder.matches("Str0ng!Passw0rd", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("yassine@example.com", "USER")).thenReturn("jwt-token");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("yassine@example.com")
                .password("Str0ng!Passw0rd")
                .build();

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("yassine@example.com");
        assertThat(response.getUsername()).isEqualTo("yassine");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("login - throws when password does not match")
    void login_invalidPassword() {
        when(userService.findByEmail("yassine@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("yassine@example.com")
                .password("wrong-password")
                .build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid password");

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login - propagates when user not found")
    void login_userNotFound() {
        when(userService.findByEmail("missing@example.com"))
                .thenThrow(new RuntimeException("User not found with email: missing@example.com"));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("missing@example.com")
                .password("whatever")
                .build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found with email: missing@example.com");
    }
}
