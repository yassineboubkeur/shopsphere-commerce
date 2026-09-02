package com.shopsphere.auth_service.service;

import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.repository.RoleRepository;
import com.shopsphere.auth_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User userWithRole(Role.RoleName name, Long id, String username, String email, String password) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password(password)
                .roles(Collections.singleton(Role.builder().id(1L).name(name).build()))
                .build();
    }

    @Test
    @DisplayName("register - creates user with encoded password and USER role")
    void register_createsUser() {
        Role userRole = Role.builder().id(1L).name(Role.RoleName.USER).build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("plainPassword")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        User result = userService.register("newbie", "user@example.com", "plainPassword");

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRoles()).extracting(r -> r.getName()).containsExactly(Role.RoleName.USER);

        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - throws when email already exists")
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("newbie", "taken@example.com", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");
    }

    @Test
    @DisplayName("register - throws when username already exists")
    void register_duplicateUsername() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("taken", "user@example.com", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");
    }

    @Test
    @DisplayName("register - throws when USER role is missing")
    void register_missingRole() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register("newbie", "user@example.com", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USER role not found");
    }

    @Test
    @DisplayName("findByEmail - returns user when found")
    void findByEmail_found() {
        User user = userWithRole(Role.RoleName.USER, 5L, "boss", "boss@example.com", "pw");
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("boss@example.com");

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getUsername()).isEqualTo("boss");
    }

    @Test
    @DisplayName("findByEmail - throws when not found")
    void findByEmail_notFound() {
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("gone@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found with email: gone@example.com");
    }

    @Test
    @DisplayName("loadUserByUsername - returns UserDetails with ROLE_ prefix")
    void loadUserByUsername_success() {
        User user = userWithRole(Role.RoleName.ADMIN, 7L, "admin", "admin@example.com", "pw");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("admin@example.com");

        assertThat(details.getUsername()).isEqualTo("admin@example.com");
        assertThat(details.getPassword()).isEqualTo("pw");
        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("updateRole - updates the user role")
    void updateRole_success() {
        User existing = userWithRole(Role.RoleName.USER, 3L, "kenza", "kenza@example.com", "pw");
        Role adminRole = Role.builder().id(2L).name(Role.RoleName.ADMIN).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName(Role.RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(3L, Role.RoleName.ADMIN);

        assertThat(updated.getRoles()).extracting(r -> r.getName()).containsExactly(Role.RoleName.ADMIN);
        verify(userRepository).save(existing);
    }
}
