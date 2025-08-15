package com.example.Campung;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@RestController
public class CampungApplication {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	public static void main(String[] args) {
		SpringApplication.run(CampungApplication.class, args);
	}

	@GetMapping("/")
	public String hello() {
		return "Hello World! 캠핑 프로젝트 시작!";
	}

	@GetMapping("/test-db")
	public String testDatabase() throws Exception {
		try (Connection conn = dataSource.getConnection()) {
			return "DB 연결 성공! Database: " + conn.getCatalog();
		}
	}

	@GetMapping("/test-redis")
	public String testRedis() {
		try {
			redisTemplate.opsForValue().set("test", "Hello Redis!");
			String value = redisTemplate.opsForValue().get("test");
			return "✅ Redis 연결 성공! 저장된 값: " + value;
		} catch (Exception e) {
			return "❌ Redis 연결 실패: Redis 서버가 실행되지 않았습니다. (localhost:6379)";
		}
	}

	@GetMapping("/test-all")
	public String testAll() {
		StringBuilder result = new StringBuilder();
		
		// DB 테스트
		try (Connection conn = dataSource.getConnection()) {
			result.append("✅ DB 연결 성공! Database: ").append(conn.getCatalog()).append("<br>");
		} catch (Exception e) {
			result.append("❌ DB 연결 실패: ").append(e.getMessage()).append("<br>");
		}
		
		// Redis 테스트
		try {
			redisTemplate.opsForValue().set("test", "Hello Redis!");
			String value = redisTemplate.opsForValue().get("test");
			result.append("✅ Redis 연결 성공! 저장된 값: ").append(value).append("<br>");
		} catch (Exception e) {
			result.append("❌ Redis 연결 실패: Redis 서버가 실행되지 않았습니다.<br>");
		}
		
		result.append("<br>🎉 연결 테스트 완료!");
		
		return result.toString();
	}
}
