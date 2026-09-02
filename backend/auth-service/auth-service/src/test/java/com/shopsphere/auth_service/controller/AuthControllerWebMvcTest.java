package com.shopsphere.auth_service.controller;

import com.shopsphere.auth_service.dto.AuthResponse;
import com.shopsphere.auth_service.dto.LoginRequest;
import com.shopsphere.auth_service.dto.RegisterRequest;
import com.shopsphere.auth_service.service.AuthService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                com.shopsphere.auth_service.security.JwtAuthenticationFilter.class,
                com.shopsphere.auth_service.security.CustomAuthenticationEntryPoint.class
        })
})
@Import(com.shopsphere.testconfig.TestSecurityConfig.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register - returns 201 with token")
    void register_returnsCreated() throws Exception {
        AuthResponse expected = AuthResponse.builder()
                .token("jwt-token")
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

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("yassine@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - missing username returns 400")
    void register_missingUsername_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("yassine@example.com")
                .password("Str0ng!Passw0rd")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("yassine")
                .email("not-an-email")
                .password("Str0ng!Passw0rd")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 200 with token")
    void login_returnsOk() throws Exception {
        AuthResponse expected = AuthResponse.builder()
                .token("jwt-token")
                .type("Bearer")
                .id(1L)
                .username("admin")
                .email("admin@shopsphere.com")
                .role("ADMIN")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(expected);

        LoginRequest request = LoginRequest.builder()
                .email("admin@shopsphere.com")
                .password("Zephyr!91Kite")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - missing email returns 400")
    void login_missingEmail_returns400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .password("Zephyr!91Kite")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}