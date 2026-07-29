package com.digniche.muntum.auth.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.mail.MailService;
import com.digniche.muntum.global.redis.EmailVerificationRedisService;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * 회원가입 이메일 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final Duration DAILY_TTL = Duration.ofHours(24);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_DAILY_SEND = 5;

    private final UserRepository userRepository;
    private final MailService mailService;
    private final EmailVerificationRedisService emailVerificationRedisService;
    private final SecureRandom secureRandom = new SecureRandom();

    // 인증번호 생성 및 이메일 발송
    public long sendCode(String email) {
        String rawEmail = email.trim();
        String normalizedEmail = normalize(rawEmail);

        // 1. 재발송 쿨다운 확인
        if (emailVerificationRedisService.isInCooldown(normalizedEmail)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_COOLDOWN);
        }

        // 2. 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmailAndStatusNot(normalizedEmail, UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. 일일 발송 횟수 확인
        long dailyCount = emailVerificationRedisService.incrementDailyCount(normalizedEmail, DAILY_TTL);
        if (dailyCount > MAX_DAILY_SEND) {
            throw new BusinessException(ErrorCode.VERIFICATION_SEND_LIMIT_EXCEEDED);
        }

        // 4. 인증번호 저장 및 발송
        String code = generateCode();
        emailVerificationRedisService.saveCode(normalizedEmail, code, CODE_TTL);
        mailService.sendSignupVerificationCode(rawEmail, code);

        // 5. 발송에 성공한 경우에만 쿨다운 설정
        emailVerificationRedisService.setCooldown(normalizedEmail, COOLDOWN_TTL);

        log.debug("회원가입 인증번호 발송: email={}", normalizedEmail);

        return CODE_TTL.getSeconds();
    }

    // 인증번호 확인 후 1회용 가입 토큰 발급
    public String verifyCode(String email, String code) {
        String normalizedEmail = normalize(email);

        String savedCode = emailVerificationRedisService.getCode(normalizedEmail);
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        long attempts = emailVerificationRedisService.incrementAttempt(normalizedEmail, CODE_TTL);
        if (attempts > MAX_ATTEMPTS) {
            emailVerificationRedisService.deleteCode(normalizedEmail);
            emailVerificationRedisService.deleteAttempt(normalizedEmail);
            throw new BusinessException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
        }

        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        emailVerificationRedisService.deleteCode(normalizedEmail);
        emailVerificationRedisService.deleteAttempt(normalizedEmail);

        String signupToken = UUID.randomUUID().toString();
        emailVerificationRedisService.saveSignupToken(signupToken, normalizedEmail, TOKEN_TTL);

        log.debug("회원가입 이메일 인증 완료: email={}", normalizedEmail);

        return signupToken;
    }

    // Redis 키는 대소문자를 구분하므로 소문자로 통일
    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 숫자 6자리 인증번호 생성
    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}