package com.shopsphere.auth_service.repository;

import com.shopsphere.auth_service.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("save - persists a role")
    void save() {
        Role role = roleRepository.save(Role.builder().name(Role.RoleName.ADMIN).build());

        assertThat(role.getId()).isNotNull();
        assertThat(role.getName()).isEqualTo(Role.RoleName.ADMIN);
    }

    @Test
    @DisplayName("findByName - returns the role")
    void findByName() {
        roleRepository.save(Role.builder().name(Role.RoleName.USER).build());

        Optional<Role> found = roleRepository.findByName(Role.RoleName.USER);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(Role.RoleName.USER);
    }

    @Test
    @DisplayName("findByName - empty for a role that was not saved")
    void findByName_notFound() {
        Optional<Role> found = roleRepository.findByName(Role.RoleName.ADMIN);

        assertThat(found).isEmpty();
    }
}