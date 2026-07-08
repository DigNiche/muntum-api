package com.digniche.muntum.global.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로컬 Redis(localhost:6379)를 대상으로 실제 TTL/증가 동작을 검증한다.
 */
class PasswordResetRedisServiceTest {

    private static LettuceConnectionFactory connectionFactory;
    private static PasswordResetRedisService service;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        service = new PasswordResetRedisService(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void 인증번호_저장후_조회하면_동일한_코드를_반환한다() {
        service.saveCode("reset-test@muntum.com", "123456", Duration.ofMinutes(5));

        assertThat(service.getCode("reset-test@muntum.com")).isEqualTo("123456");
    }

    @Test
    void 인증번호_삭제후_조회하면_null이다() {
        service.saveCode("delete-test@muntum.com", "111111", Duration.ofMinutes(5));

        service.deleteCode("delete-test@muntum.com");

        assertThat(service.getCode("delete-test@muntum.com")).isNull();
    }

    @Test
    void 시도횟수는_호출할때마다_증가한다() {
        String email = "attempt-test@muntum.com";
        service.deleteAttempt(email);

        long first = service.incrementAttempt(email, Duration.ofMinutes(5));
        long second = service.incrementAttempt(email, Duration.ofMinutes(5));
        long third = service.incrementAttempt(email, Duration.ofMinutes(5));

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(2);
        assertThat(third).isEqualTo(3);
    }

    @Test
    void 재설정_토큰을_저장하면_이메일로_역참조된다() {
        service.saveResetToken("token-abc", "token-test@muntum.com", Duration.ofMinutes(10));

        assertThat(service.getEmailByResetToken("token-abc")).isEqualTo("token-test@muntum.com");

        service.deleteResetToken("token-abc");
        assertThat(service.getEmailByResetToken("token-abc")).isNull();
    }

    @Test
    void 인증번호_TTL이_만료되면_null이다() throws InterruptedException {
        service.saveCode("ttl-test@muntum.com", "999999", Duration.ofMillis(500));

        Thread.sleep(700);

        assertThat(service.getCode("ttl-test@muntum.com")).isNull();
    }
}
