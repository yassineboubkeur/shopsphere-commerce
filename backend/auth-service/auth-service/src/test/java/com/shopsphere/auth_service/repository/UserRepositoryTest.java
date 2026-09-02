package com.shopsphere.auth_service.repository;

import com.shopsphere.auth_service.entity.Role;
import com.shopsphere.auth_service.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("saveAndFind - persists and retrieves a user by id")
    void saveAndFindById() {
        User saved = userRepository.save(buildUser());

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("yassine@example.com");
    }

    @Test
    @DisplayName("findByEmail - returns the user")
    void findByEmail() {
        userRepository.save(buildUser());

        Optional<User> found = userRepository.findByEmail("yassine@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("yassine");
    }

    @Test
    @DisplayName("findByUsername - returns the user")
    void findByUsername() {
        userRepository.save(buildUser());

        Optional<User> found = userRepository.findByUsername("yassine");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("yassine@example.com");
    }

    @Test
    @DisplayName("existsByEmail - true for existing email, false otherwise")
    void existsByEmail() {
        userRepository.save(buildUser());

        assertThat(userRepository.existsByEmail("yassine@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("existsByUsername - true for existing username, false otherwise")
    void existsByUsername() {
        userRepository.save(buildUser());

        assertThat(userRepository.existsByUsername("yassine")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    @DisplayName("saveUserWithRoles - persists the user's roles through the join table")
    void saveUserWithRoles() {
        Role admin = roleRepository.save(Role.builder().name(Role.RoleName.ADMIN).build());
        Role userRole = roleRepository.save(Role.builder().name(Role.RoleName.USER).build());

        User user = User.builder()
                .username("yassine")
                .email("yassine@example.com")
                .password("encoded-password")
                .roles(Set.of(admin, userRole))
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail(user.getEmail());

        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).hasSize(2);
        assertThat(found.get().getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(Role.RoleName.ADMIN, Role.RoleName.USER);
    }

    private User buildUser() {
        Role userRole = roleRepository.save(Role.builder().name(Role.RoleName.USER).build());
        return User.builder()
                .username("yassine")
                .email("yassine@example.com")
                .password("encoded-password")
                .roles(Set.of(userRole))
                .build();
    }
}