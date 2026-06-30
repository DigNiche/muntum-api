package com.digniche.muntum.user.service;

import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.user.dto.NicknameUpdateRequest;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public static final String MASKING_LETTER_PREFIX = "_del_";
    private static final int DATA_RETENTION_DISPOSAL_YEAR = 5;
    private static final String WITHDRAWAL_NICKNAME_PREFIX = "탈퇴회원";

    // 닉네임 설정(생성 및 수정)
    @Transactional
    public void setNickname(UUID userId, NicknameUpdateRequest request) {
        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.nickname());
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(UUID userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // TODO: 프로그램 제보 내역, 댓글, 이메일, 유저 약관 동의, 가입일/탈퇴일 제외 모두(키워드, 검색어 기록) 연관 삭제
        // TODO: 프로그램 제보 내역, 댓글 작성자/수정자 null 처리
        // TODO: 프로필 이미지 삭제

        // 탈퇴 후 재가입을 위한 이메일 마스킹 처리(비식별화). DB의 Email 컬럼 Unique 제약 유지.
        String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
        user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
        user.softDelete(userId);

        refreshTokenService.delete(userId);
        // TODO: 완전한 즉시 로그아웃 필요
    }

    // 회원 탈퇴 N년 후 정보 완전 파기
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredWithdrawalUsers() {
        LocalDateTime ago = LocalDateTime.now().minusYears(DATA_RETENTION_DISPOSAL_YEAR);


        // 탈퇴 후 DATA_RETENTION_DISPOSAL_YEAR 기간이 지났고, 여전히 DELETED 상태인 유저들만 완전 삭제
        userRepository.deleteByStatusAndDeletedAtBefore(UserStatus.DELETED, ago);
        // TODO: 유저 약관 동의 삭제
    }
}
