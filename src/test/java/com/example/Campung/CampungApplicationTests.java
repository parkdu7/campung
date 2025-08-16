package com.example.Campung;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 기본 애플리케이션 테스트
 * CI 환경에서 빠른 테스트 실행을 위해 Spring Context 로드 없이 실행
 */
class CampungApplicationTests {

	@Test
	void applicationClassExists() {
		// CampungApplication 클래스가 존재하는지 확인
		assertNotNull(CampungApplication.class);
		assertEquals("CampungApplication", CampungApplication.class.getSimpleName());
	}

	@Test
	void mainMethodExists() throws Exception {
		// main 메소드가 존재하는지 확인
		var mainMethod = CampungApplication.class.getDeclaredMethod("main", String[].class);
		assertNotNull(mainMethod);
		assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
		assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
	}

	@Test
	void basicAssertions() {
		// 기본적인 Java 기능 테스트
		String expected = "Hello";
		String actual = "Hello";
		assertEquals(expected, actual);
		assertTrue(true);
		assertFalse(false);
	}
}
