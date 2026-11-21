package com.digniche.muntum.user.service;

import com.digniche.muntum.Announcement.repository.AnnouncementRepository;
import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.auth.service.AccessTokenService;
import com.digniche.muntum.auth.service.AuthService;
import com.digniche.muntum.curationlike.service.CurationLikeService;
import com.digniche.muntum.curator.repository.CuratorApplicationRepository;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.scrap.repository.ScrapRepository;
import com.digniche.muntum.suggestion.repository.SpotSuggestionRepository;
import com.digniche.muntum.user.dto.request.NicknameUpdateRequest;
import com.digniche.muntum.user.dto.request.PasswordChangeRequest;
import com.digniche.muntum.user.dto.request.TermsConsentListRequest;
import com.digniche.muntum.user.dto.request.TermsConsentRequest;
import com.digniche.muntum.user.dto.response.UserConsentStatusResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.entity.UserTermsAgreement;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.user.dto.response.UserProfileDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.auth.social.AppleTokenClient;
import com.digniche.muntum.auth.social.AppleTokenResponse;
import com.digniche.muntum.auth.social.AppleTokenVerifier;
import com.digniche.muntum.auth.social.SocialTokenCipher;
import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.user.entity.SocialAccount;
import com.digniche.muntum.user.entity.SocialProvider;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.UUID;
import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;
import java.util.EnumSet;
import com.digniche.muntum.auth.social.KakaoTokenVerifier;
import com.digniche.muntum.auth.social.KakaoTokenClient;

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
    private final CuratorApplicationRepository curatorApplicationRepository;
    private final TermsRepository termsRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final AuthService authService;
    private final UserProfileImageService userProfileImageService;
    private final CurationLikeService curationLikeService;
    private final CurationService curationService;
    private final CurationRepository curationRepository;
    private final CurationImageRepository curationImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppleTokenVerifier appleTokenVerifier;
    private final AppleTokenClient appleTokenClient;
    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final KakaoTokenClient kakaoTokenClient;
    private final SocialTokenCipher socialTokenCipher;
    private final PlatformTransactionManager transactionManager;

    public static final String MASKING_LETTER_PREFIX = "_del_";
    private static final int DATA_RETENTION_DISPOSAL_YEAR = 5;
    private static final String WITHDRAWAL_NICKNAME_PREFIX = "탈퇴회원";
    private static final UUID SYSTEM_UUID = AuditorAwareImpl.SYSTEM_UUID;

    /**
     * 내 프로필 조회
     */
    @Transactional(readOnly = true)
    public UserProfileDetailResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toProfileResponses(List.of(user)).get(0);
    }


    /**
     * 전체 사용자 목록 조회  - 검색(닉네임/이메일)
     */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileDetailResponse> getUsers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> users = (search == null || search.isBlank())
                ? userRepository.findAllByStatusNot(UserStatus.DELETED, pageable)
                : userRepository.searchByNicknameOrEmail(search.trim(), UserStatus.DELETED, pageable);

        List<UserProfileDetailResponse> content = toProfileResponses(users.getContent());
        return PageResponse.from(new PageImpl<>(content, pageable, users.getTotalElements()));
    }


    /**
     * 닉네임 설정(생성, 수정)
     */
    @Transactional
    public void setNickname(UUID userId, NicknameUpdateRequest request) {
        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.nickname());
    }


    /**
     * 마이페이지에서 선택 약관 동의 상태 변경
     *
     * 동의:
     *   현재 공개된 문서의 버전 필요
     *
     * 철회:
     *   원래 약관 문서가 더 이상 활성화되지 않은 과거 동의 기록도 언제든 철회 가능
     */
    @Transactional
    public void updateTermsConsent(
            UUID userId,
            TermsConsentListRequest request
    ) {
        // 동시에 발생한 요청들이 서로의 약관 버전 정보를 덮어쓰지 않도록 현재 기록을 락
        UserTermsAgreement agreement =
                userTermsAgreementRepository
                        .findTopByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        EnumSet<UserTermsType> submittedTypes =
                EnumSet.noneOf(UserTermsType.class);

        LocalDateTime recordedAt = LocalDateTime.now();

        for (TermsConsentRequest choice : request.terms()) {

            if (choice == null || choice.termType() == null) {
                throw new BusinessException(ErrorCode.INVALID_TERMS_CONSENT_UPDATE);
            }

            UserTermsType type = choice.termType();

            // 동일한 약관 유형을 중복 제출할 수 없다.
            if (!submittedTypes.add(type)) {
                throw new BusinessException(ErrorCode.INVALID_TERMS_CONSENT_UPDATE);
            }

            // 서비스 이용약관과 개인정보처리방침은
            // 선택 약관 설정에서 변경할 수 없다.
            if (type.requiresSignupConsent()
                    || type.isPrivacyNotice()) {
                throw new BusinessException(ErrorCode.INVALID_TERMS_CONSENT_UPDATE);
            }

            if (!choice.agreed()) {

                // 동의 철회에는 활성 문서나 버전이 필요하지 않음
                // 이를 통해 과거 위치정보 동의 철회 권한 유지
                agreement.updateOptionalConsent(type, false, null, recordedAt);

                continue;
            }
            // 구버전 앱 위치 접근 권한 호환 처리
            // TODO: 구버전 앱 지원 종료 후 제거
            if (type == UserTermsType.LOCATION_TERMS && (choice.version() == null || choice.version().isBlank())) {
                agreement.recordLegacyLocationPermission(recordedAt);

                continue;
            }
            // 새로운 동의에는 실제로 존재하는 문서 필요
            Terms published = termsRepository
                    .findByTypeAndActiveTrueAndDeletedAtIsNull(type)
                    .orElseThrow(() -> new BusinessException(ErrorCode.OPTIONAL_TERMS_NOT_AVAILABLE));

            if (!TermsAvailability.canCollectConsent(published, recordedAt)) {
                throw new BusinessException(ErrorCode.OPTIONAL_TERMS_NOT_AVAILABLE);
            }

            if (choice.version() == null
                    || choice.version().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_TERMS_CONSENT_UPDATE);
            }

            if (!published.getVersion().equals(choice.version())) {
                throw new BusinessException(ErrorCode.TERMS_VERSION_MISMATCH);
            }

            // 문서 버전 검증 완료
            agreement.updateOptionalConsent(type, true, published.getVersion(), recordedAt
            );
        }

        // 명시적으로 save()를 호출할 필요X, 이 엔티티는 현재 트랜잭션에서 관리, 실제 변경 사항은 JPA 변경 감지로 저장
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.delete(userId);  // 다른 기기 세션 로그아웃 (재로그인 유도)
    }

    /**
     * 회원 탈퇴 N년 후 정보 완전 파기
     * @Scheduled(cron = "0 0 3 * * *")
     */
    @Transactional
    public void deleteExpiredWithdrawalUsers() {
        LocalDateTime ago = LocalDateTime.now().minusYears(DATA_RETENTION_DISPOSAL_YEAR);

        // TODO: 사용자 데이터 완전 삭제

        // 탈퇴 후 DATA_RETENTION_DISPOSAL_YEAR 기간이 지났고, 여전히 DELETED 상태인 유저들만 완전 삭제
        userRepository.deleteByStatusAndDeletedAtBefore(UserStatus.DELETED, ago);
    }

    /**
     * 회원 탈퇴
     */
    public void withdraw(UUID userId, WithdrawRequest request, String accessToken) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        SocialAccount socialAccount = socialAccountRepository.findByUserId(userId).orElse(null);

        if (socialAccount == null) { verifyPasswordWithdrawal(user, request); }
        else {
            verifySocialWithdrawal(socialAccount, request);
            log.info(
                    "[SOCIAL_WITHDRAW_REAUTH_AND_REVOKE_COMPLETED] provider={}, userId={}",
                    socialAccount.getProvider(),
                    userId
            );
        }

        // 외부 인증 및 연결 해제 완료 후 DB 탈퇴 처리
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            transactionTemplate.executeWithoutResult(
                    status -> completeWithdrawal(user)
            );
        } catch (RuntimeException exception) {
            log.error(
                    "[WITHDRAW_TRANSACTION_ERROR] userId={}, provider={}, exceptionType={}",
                    userId,
                    socialAccount == null
                            ? "EMAIL"
                            : socialAccount.getProvider(),
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
        log.info("[WITHDRAW_DB_DELETE_COMPLETED] userId={}",userId);

        // MySQL 회원 삭제가 커밋된 이후 Redis 세션 삭제
        try {
            refreshTokenService.delete(userId);
        } catch (RuntimeException exception) {
            log.error(
                    "[WITHDRAW_REFRESH_TOKEN_DELETE_FAILED] userId={}",
                    userId
            );
        }

        try {
            long remainingMillis =
                    authService.calculateTokenTtl(accessToken);

            if (remainingMillis > 0) {
                accessTokenService.addToWithdrawalList(
                        accessToken,
                        remainingMillis
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "[WITHDRAW_ACCESS_TOKEN_INVALIDATION_FAILED] userId={}",
                    userId
            );
        }
    }


    /**
     * 조회된 사용자들의 키워드/제보/스크랩 개수를 집계하여 응답 DTO로 변환
     */
    private List<UserProfileDetailResponse> toProfileResponses(List<User> users) {
        if (users.isEmpty()) { return List.of(); }
        List<UUID> userIds = users.stream().map(User::getId).toList();

        Map<UUID, Long> keywordCounts = toCountMap(userKeywordRepository.countActiveByUserIds(userIds));
        Map<UUID, Long> suggestionCounts = toCountMap(spotSuggestionRepository.countByInformerIds(userIds));
        Map<UUID, Long> scrapCounts = toCountMap(scrapRepository.countByUserIds(userIds));

        return users.stream().map(user -> UserProfileDetailResponse.from(
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

    /**
     * 일반 가입 회원 검증
     */
    private void verifyPasswordWithdrawal(User user, WithdrawRequest request) {
        if (request.password() == null || request.password().isBlank() || user.getPassword() == null
                || !passwordEncoder.matches(request.password(),user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }

    /**
     * 소셜 가입 회원 apple 재인증 및 철회
     */
    private void verifySocialWithdrawal(SocialAccount socialAccount, WithdrawRequest request) {

        // Kakao 회원 탈퇴
        if (socialAccount.getProvider() == SocialProvider.KAKAO) {
            verifyKakaoWithdrawal(socialAccount, request);
            return;
        }

        // 기존 Apple 회원 탈퇴
        if (socialAccount.getProvider() != SocialProvider.APPLE) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }

        if (request.token() == null || request.token().isBlank() || request.authorizationCode() == null || request.authorizationCode().isBlank()
                || request.nonce() == null || request.nonce().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_REAUTHENTICATION_REQUIRED);
        }

        // 1. 앱에서 직접 받은 Identity Token 검증
        SocialUserInfo requestedUser =
                appleTokenVerifier.verify(new SocialLoginRequest(SocialProvider.APPLE, request.token(), request.authorizationCode(), request.nonce()));

        // 2. authorizationCode를 Apple 서버와 교환
        AppleTokenResponse tokenResponse = appleTokenClient.exchangeAuthorizationCode(request.authorizationCode());

        if (tokenResponse.idToken() == null || tokenResponse.idToken().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 3. 교환 결과로 받은 id_token도 다시 검증
        SocialUserInfo exchangedUser =
                appleTokenVerifier.verify(new SocialLoginRequest(SocialProvider.APPLE, tokenResponse.idToken(), null, request.nonce()));

        String savedProviderUserId = socialAccount.getProviderUserId();

        // 사용자 식별자(3개)가 모두 같아야 함:  DB에 저장된 Apple sub == 앱이 보낸 Identity Token의 sub == authorizationCode 교환 결과의 sub
        if (!savedProviderUserId.equals(requestedUser.providerUserId()) || !savedProviderUserId.equals(exchangedUser.providerUserId())) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        String refreshToken = resolveRefreshTokenForRevocation(socialAccount, tokenResponse);

        // DB 삭제 전, Apple 연결부터 철회
        appleTokenClient.revokeRefreshToken(refreshToken);
    }

    /**
     * Kakao 소셜 회원 재인증 및 연결 해제
     */
    private void verifyKakaoWithdrawal(
            SocialAccount socialAccount,
            WithdrawRequest request
    ) {
        // 1. Kakao 재인증 정보 확인
        if (request.token() == null || request.token().isBlank()) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_REAUTHENTICATION_REQUIRED
            );
        }

        // 2. Kakao Access Token 및 앱 소유권 검증
        SocialUserInfo requestedUser = kakaoTokenVerifier.verify(
                new SocialLoginRequest(
                        SocialProvider.KAKAO,
                        request.token(),
                        null,
                        null
                )
        );

        // 3. 실제 탈퇴 요청 계정과 Kakao 사용자 ID 비교
        String savedProviderUserId = socialAccount.getProviderUserId();

        if (!savedProviderUserId.equals(requestedUser.providerUserId())) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 4. DB 삭제 전 Kakao 연결 해제
        kakaoTokenClient.unlink(
                request.token(),
                savedProviderUserId
        );
    }


    /**
     * 철회할 refresh token 선택 메서드 추가
     */
    private String resolveRefreshTokenForRevocation(SocialAccount socialAccount, AppleTokenResponse tokenResponse) {

        // 탈퇴 재인증 과정에서 새로 발급된 Refresh Token을 우선 사용
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()) {
            return tokenResponse.refreshToken();
        }

        // 새 토큰이 없다면 로그인 때, DB에 저장한 암호문을 복호화
        String encryptedRefreshToken = socialAccount.getProviderRefreshToken();

        if (encryptedRefreshToken == null || encryptedRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
        }

        return socialTokenCipher.decrypt(encryptedRefreshToken);
    }


    /**
     * 탈퇴 사용자에 대한 처리
     */
    private void completeWithdrawal(User user) {
        UUID userId = user.getId();
//        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // User 행 배타 잠금. 잠금 조회 후 일반 조회 시 스냅샷 생성. Lock 순서 : User -> Curation -> Curation Like
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 큐레이션 좋아요 삭제 및 좋아요 수 감소
        curationLikeService.clearLikeActivity(userId);

        String role = user.getRole().name();
        UUID withdrawnUuid = AuditorAwareImpl.toWithdrawnUserUuid(role, userId);

        switch (role) {
            case "MANAGER" -> {
                // 제보의 생성/수정/검토자/제보자/삭제자는 탈퇴자 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceReviewedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceInformerWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceDeletedByWith(userId, withdrawnUuid);

                // 키워드의 생성/수정/삭제자 시스템 UUID 처리
                keywordRepository.replaceCreatedByWith(userId, withdrawnUuid);
                keywordRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                keywordRepository.replaceDeletedByWith(userId, withdrawnUuid);

                // 프로그램의 생성자/수정자/삭제자 시스템 UUID 처리
                programRepository.replaceCreatedByWith(userId, withdrawnUuid);
                programRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                programRepository.replaceDeletedByWith(userId, withdrawnUuid);

                // 공지의 생성자/수정자/삭제자
                announcementRepository.replaceCreatedByWith(userId, withdrawnUuid);
                announcementRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                announcementRepository.replaceDeletedByWith(userId, withdrawnUuid);

                // 약관의 생성자/수정자/삭제자
                termsRepository.replaceCreatedByWith(userId, withdrawnUuid);
                termsRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                termsRepository.replaceDeletedByWith(userId, withdrawnUuid);

                // 큐레이터 지원서의 검토자/수정자
                curatorApplicationRepository.replaceReviewedByWith(userId, withdrawnUuid);
                curatorApplicationRepository.replaceUpdatedByWith(userId, withdrawnUuid);

            }
            case "CURATOR" -> {
                // 제보의 생성자/수정자/제보자는 탈퇴자 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceInformerWith(userId, withdrawnUuid);

                // 프로그램의 생성자/수정자 Null 처리 및 시스템 UUID 처리
                programRepository.replaceCreatedByWith(userId, withdrawnUuid);
                programRepository.replaceUpdatedByWith(userId, withdrawnUuid);

            }
            case "AUDIENCE" -> {
                // 제보의 생성자/수정자/제보자는 탈퇴자 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceInformerWith(userId, withdrawnUuid);
            }
        }

        // 사용자 관련 데이터 삭제
        userTermsAgreementRepository.deleteAllByUserId(userId);
        curatorApplicationRepository.deleteAllByUserId(userId);

        // social_accounts가 users를 외래키로 참조하므로 users보다 먼저 삭제
        socialAccountRepository.deleteAllByUserId(userId);

        /*
         탈퇴 후 재가입을 위한 이메일 마스킹 처리(비식별화). DB의 Email 컬럼 Unique 제약 유지.
                String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
                user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
                user.softDelete(userId);

                String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
                user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
                user.softDelete(userId);
         현재 Access Token 블랙리스트 등록
         */

        /* 회원 탈퇴 N년 후 정보 완전 파기
            @Scheduled(cron = "0 0 3 * * *")
        */

        // 사용자 객체 삭제 : [ CASCADE DELETE ] 스크랩(scraps), 관심 키워드(user_keywords), 프로그램 반응(program_reactions)
        userRepository.delete(user);

        // 프로필 이미지 스토리지에서 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userProfileImageService.deleteStoredImage(user.getProfileImageUrl());
            }
        });
    }

    @Transactional(readOnly = true)
    public UserConsentStatusResponse getTermsConsent(UUID userId) {
        UserTermsAgreement agreement =
                userTermsAgreementRepository
                        .findTopByUserIdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.TERMS_NOT_FOUND
                                )
                        );

        return UserConsentStatusResponse.from(agreement);
    }
}