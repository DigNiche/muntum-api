package com.digniche.muntum.auth.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.mail.MailService;
import com.digniche.muntum.global.redis.PasswordResetRedisService;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * 비밀번호 찾기 / 재설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordResetRedisService passwordResetRedisService;
    private final SecureRandom secureRandom = new SecureRandom();

    // 인증번호 생성 및 이메일 발송
    @Transactional(readOnly = true)
    public void sendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = generateCode();
        passwordResetRedisService.saveCode(user.getEmail(), code, CODE_TTL);
        mailService.sendPasswordResetCode(user.getEmail(), code);

        log.debug("비밀번호 재설정 인증번호 발송: email={}", user.getEmail());
    }

    // 인증번호 확인 후 1회용 재설정 토큰 발급
    public String verifyCode(String email, String code) {
        String savedCode = passwordResetRedisService.getCode(email);
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        long attempts = passwordResetRedisService.incrementAttempt(email, CODE_TTL);
        if (attempts > MAX_ATTEMPTS) {
            passwordResetRedisService.deleteCode(email);
            passwordResetRedisService.deleteAttempt(email);
            throw new BusinessException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
        }

        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        passwordResetRedisService.deleteCode(email);
        passwordResetRedisService.deleteAttempt(email);

        String resetToken = UUID.randomUUID().toString();
        passwordResetRedisService.saveResetToken(resetToken, email, TOKEN_TTL);
        return resetToken;
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = passwordResetRedisService.getEmailByResetToken(resetToken);
        if (email == null) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(newPassword));
        passwordResetRedisService.deleteResetToken(resetToken);

        log.debug("비밀번호 재설정 완료: email={}", email);
    }

    // 숫자 6자리 인증번호 생성
    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
