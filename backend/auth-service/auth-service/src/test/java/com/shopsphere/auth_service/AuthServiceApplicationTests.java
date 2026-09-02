package com.shopsphere.auth_service;

import com.shopsphere.auth_service.config.AdminInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AuthServiceApplicationTests {

	@MockitoBean
	private AdminInitializer adminInitializer;

	@Test
	void contextLoads() {
	}

}
