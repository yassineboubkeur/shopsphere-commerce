package com.shopsphere.auth_service.integration;

import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import com.shopsphere.auth_service.repository.RoleRepository;
import com.shopsphere.auth_service.repository.UserRepository;
import com.shopsphere.auth_service.service.UserService;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRegistrationIT extends PostgresTestContainer {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(Role.RoleName.USER).build());
        roleRepository.save(Role.builder().name(Role.RoleName.ADMIN).build());
    }

    @Test
    @DisplayName("register - creates user with USER role in PostgreSQL")
    void register_createsUserInDatabase() {
        User saved = userService.register("yassine", "yassine@test.com", "SecurePass123!");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("yassine");
        assertThat(saved.getEmail()).isEqualTo("yassine@test.com");
        assertThat(passwordEncoder.matches("SecurePass123!", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).hasSize(1);
        assertThat(saved.getRoles().iterator().next().getName()).isEqualTo(Role.RoleName.USER);
    }

    @Test
    @DisplayName("register - throws when email already exists")
    void register_duplicateEmail_throwsException() {
        userService.register("user1", "dup@test.com", "SecurePass123!");

        assertThatThrownBy(() -> userService.register("user2", "dup@test.com", "OtherPass456!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("register - throws when username already exists")
    void register_duplicateUsername_throwsException() {
        userService.register("dupuser", "first@test.com", "SecurePass123!");

        assertThatThrownBy(() -> userService.register("dupuser", "second@test.com", "OtherPass456!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("findByEmail - retrieves user from PostgreSQL")
    void findByEmail_returnsUser() {
        userService.register("yassine", "yassine@test.com", "SecurePass123!");

        User found = userService.findByEmail("yassine@test.com");

        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("yassine");
    }

    @Test
    @DisplayName("updateRole - changes user role in PostgreSQL")
    void updateRole_changesRoleInDatabase() {
        User saved = userService.register("yassine", "yassine@test.com", "SecurePass123!");
        assertThat(saved.getRoles().iterator().next().getName()).isEqualTo(Role.RoleName.USER);

        User updated = userService.updateRole(saved.getId(), Role.RoleName.ADMIN);

        assertThat(updated.getRoles()).hasSize(1);
        assertThat(updated.getRoles().iterator().next().getName()).isEqualTo(Role.RoleName.ADMIN);
    }

    @Test
    @DisplayName("delete - removes user from PostgreSQL")
    void delete_removesUserFromDatabase() {
        User saved = userService.register("yassine", "yassine@test.com", "SecurePass123!");

        userService.delete(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}
