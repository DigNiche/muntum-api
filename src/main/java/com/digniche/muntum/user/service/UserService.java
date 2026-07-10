package com.digniche.muntum.user.service;

import com.digniche.muntum.Announcement.repository.AnnouncementRepository;
import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.auth.service.AccessTokenService;
import com.digniche.muntum.auth.service.AuthService;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.scrap.repository.ScrapRepository;
import com.digniche.muntum.suggestion.repository.SpotSuggestionRepository;
import com.digniche.muntum.user.dto.NicknameUpdateRequest;
import com.digniche.muntum.user.dto.PasswordChangeRequest;
import com.digniche.muntum.user.dto.TermsConsentListRequest;
import com.digniche.muntum.user.dto.TermsConsentRequest;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.entity.UserTermsAgreement;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final SpotSuggestionRepository spotSuggestionRepository;
    private final ScrapRepository scrapRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final ProgramRepository programRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final AuthService authService;

    public static final String MASKING_LETTER_PREFIX = "_del_";
    private static final int DATA_RETENTION_DISPOSAL_YEAR = 5;
    private static final String WITHDRAWAL_NICKNAME_PREFIX = "탈퇴회원";
    private static final UUID SYSTEM_UUID = AuditorAwareImpl.SYSTEM_UUID;

    // 내 프로필 조회 (마이페이지 프로필 + 계정관리 공용)
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toProfileResponses(List.of(user)).get(0);
    }

    // 사용자 관리 - 사용자 목록 조회 (관리자 전용, 닉네임/이메일 검색)
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> getUsers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> users = (search == null || search.isBlank())
                ? userRepository.findAllByStatusNot(UserStatus.DELETED, pageable)
                : userRepository.searchByNicknameOrEmail(search.trim(), UserStatus.DELETED, pageable);

        List<UserProfileResponse> content = toProfileResponses(users.getContent());
        return PageResponse.from(new PageImpl<>(content, pageable, users.getTotalElements()));
    }

    // 조회된 사용자들의 키워드/제보/스크랩 개수를 집계하여 응답 DTO로 변환
    private List<UserProfileResponse> toProfileResponses(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<UUID> userIds = users.stream().map(User::getId).toList();

        Map<UUID, Long> keywordCounts = toCountMap(userKeywordRepository.countActiveByUserIds(userIds));
        Map<UUID, Long> suggestionCounts = toCountMap(spotSuggestionRepository.countByInformerIds(userIds));
        Map<UUID, Long> scrapCounts = toCountMap(scrapRepository.countByUserIds(userIds));

        return users.stream()
                .map(user -> UserProfileResponse.of(
                        user,
                        keywordCounts.getOrDefault(user.getId(), 0L),
                        suggestionCounts.getOrDefault(user.getId(), 0L),
                        scrapCounts.getOrDefault(user.getId(), 0L)
                ))
                .toList();
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    // 닉네임 설정(생성 및 수정)
    @Transactional
    public void setNickname(UUID userId, NicknameUpdateRequest request) {
        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.nickname());
    }

    // 사용자 약관 정보 변경
    @Transactional
    public void updateTermsConsent(UUID userId, TermsConsentListRequest request) {
        UserTermsAgreement terms = userTermsAgreementRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        for (TermsConsentRequest consent : request.terms()) {
            if (consent.termType().isRequired()) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_CANNOT_DISAGREE);
            }
            if (consent.agreed()) {
                terms.agreeTerm(consent.termType());
            }
            else {
                Boolean isOptOutAllowed = terms.disagreeTerm(consent.termType());
            }
        }
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(UUID userId, WithdrawRequest request, String accessToken) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        UserRole role = user.getRole();

        if (role == UserRole.MANAGER) {
            // 제보의 생성/수정/검토자/제보자 시스템 UUID 처리
            spotSuggestionRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceReviewedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceInformerWithSystem(userId, SYSTEM_UUID);
            // 키워드의 생성/수정/삭제자 시스템 UUID 처리
            keywordRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            keywordRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            keywordRepository.replaceDeletedByWithSystem(userId, SYSTEM_UUID);
            // 프로그램의 생성자/수정자/삭제자 시스템 UUID 처리
            programRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            programRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            programRepository.replaceDeletedByWithSystem(userId, SYSTEM_UUID);
            // 공지의 생성자/수정자/삭제자
            announcementRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            announcementRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            announcementRepository.replaceDeletedByWithSystem(userId, SYSTEM_UUID);
        } else if (role == UserRole.CURATOR) {
            // 제보의 생성자/수정자/제보자 시스템 UUID 처리
            spotSuggestionRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceInformerWithSystem(userId, SYSTEM_UUID);
            // 프로그램의 생성자/수정자 Null 처리 및 시스템 UUID 처리
            programRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            programRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);

        } else { // AUDIENCE
            // 제보의 생성자/수정자/제보자 Null 처리 및 시스템 UUID 처리
            spotSuggestionRepository.replaceCreatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceUpdatedByWithSystem(userId, SYSTEM_UUID);
            spotSuggestionRepository.replaceInformerWithSystem(userId, SYSTEM_UUID);
        }

        // 사용자 데이터 삭제 : 약관 동의 이력, 스크랩, 취향 키어드
        userTermsAgreementRepository.deleteAllByUserId(userId);
        scrapRepository.deleteAllByUserId(userId);
        userKeywordRepository.deleteAllByUserId(userId);
        // SearchHistory: 비활성화 상태 — 활성화 시 추가


        // 탈퇴 후 재가입을 위한 이메일 마스킹 처리(비식별화). DB의 Email 컬럼 Unique 제약 유지.
//        String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
//        user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
//        user.softDelete(userId);

        long remainingMillis = authService.calculateTokenTtl(accessToken);
        if (remainingMillis > 0) {
            accessTokenService.addToWithdrawlList(accessToken, remainingMillis);
        }
        refreshTokenService.delete(userId);
        userRepository.delete(user);
    }



    // 회원 탈퇴 N년 후 정보 완전 파기
//    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredWithdrawalUsers() {
        LocalDateTime ago = LocalDateTime.now().minusYears(DATA_RETENTION_DISPOSAL_YEAR);

        // TODO: 사용자 데이터 완전 삭제

        // 탈퇴 후 DATA_RETENTION_DISPOSAL_YEAR 기간이 지났고, 여전히 DELETED 상태인 유저들만 완전 삭제
        userRepository.deleteByStatusAndDeletedAtBefore(UserStatus.DELETED, ago);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);  // 기존 A003 재사용
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.delete(userId);  // 다른 기기 세션 로그아웃 (재로그인 유도)
    }
}