package com.example.Campung;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // test 프로파일 활성화 - H2 DB 사용, Redis 비활성화
class CampungApplicationTests {

	@Test
	void contextLoads() {
		// Spring Context가 정상적으로 로드되는지 테스트
		// H2 인메모리 DB 사용, Redis는 비활성화
	}

}
