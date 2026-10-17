package com.pulsepass;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//
//@Import(TestcontainersConfiguration.class)
//@SpringBootTest
//class PulsepassApplicationTests {
//
//	@Test
//	void contextLoads() {
//	}
//
//}

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"pulsepass.security.jwt.secret-base64=dGVzdC1zZWNyZXQta2V5LXBhcmEtcHVsc2VwYXNzLXRlc3RzLTEyMzQ1Njc4OTA="
})
class PulsepassApplicationTests {

	@Test
	void contextLoads() {
	}
}
