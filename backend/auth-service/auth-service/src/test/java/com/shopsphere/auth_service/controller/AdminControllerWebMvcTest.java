package com.shopsphere.auth_service.controller;

import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                com.shopsphere.auth_service.security.JwtAuthenticationFilter.class,
                com.shopsphere.auth_service.security.CustomAuthenticationEntryPoint.class
        })
})
@Import(com.shopsphere.testconfig.TestSecurityConfig.class)
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/admin/dashboard - admin sees dashboard")
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    void dashboard_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Welcome to Admin Dashboard!"))
                .andExpect(jsonPath("$.email").value("admin@shopsphere.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - non-admin returns 403")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void dashboard_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users - admin returns user list")
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    void getAllUsers_asAdmin_returnsList() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("yassine")
                .email("yassine@example.com")
                .roles(Set.of(Role.builder().name(Role.RoleName.USER).build()))
                .createdAt(LocalDateTime.now())
                .build();
        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("yassine@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/role - admin updates user role")
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    void updateRole_asAdmin_returns200() throws Exception {
        User updated = User.builder()
                .id(2L)
                .username("other")
                .email("other@example.com")
                .roles(Set.of(Role.builder().name(Role.RoleName.ADMIN).build()))
                .build();
        when(userService.updateRole(2L, Role.RoleName.ADMIN)).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/2/role")
                        .contentType("application/json")
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/role - invalid role name returns 400")
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    void updateRole_invalidRole_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/role")
                        .contentType("application/json")
                        .content("{\"role\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateRole(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - admin deletes user returns 204")
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    void deleteUser_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/users/3"))
                .andExpect(status().isNoContent());

        verify(userService).delete(3L);
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - non-admin returns 403")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/3"))
                .andExpect(status().isForbidden());

        verify(userService, never()).delete(any());
    }
}