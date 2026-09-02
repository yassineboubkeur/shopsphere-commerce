package com.shopsphere.auth_service.controller;

import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.service.UserService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                com.shopsphere.auth_service.security.JwtAuthenticationFilter.class,
                com.shopsphere.auth_service.security.CustomAuthenticationEntryPoint.class
        })
})
@Import(com.shopsphere.testconfig.TestSecurityConfig.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/user/profile - authenticated user returns their profile")
    @WithMockUser(username = "yassine@example.com", roles = {"USER"})
    void getProfile_returnsProfile() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("yassine")
                .email("yassine@example.com")
                .roles(Set.of(Role.builder().name(Role.RoleName.USER).build()))
                .build();
        when(userService.findByEmail("yassine@example.com")).thenReturn(user);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yassine@example.com"))
                .andExpect(jsonPath("$.username").value("yassine"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/user/profile - unauthenticated request returns 401/403")
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }
}