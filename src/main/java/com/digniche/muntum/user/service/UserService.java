package com.digniche.muntum.user.service;

import com.digniche.muntum.Announcement.repository.AnnouncementRepository;
import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.auth.service.AccessTokenService;
import com.digniche.muntum.auth.service.AuthService;
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
import com.digniche.muntum.user.dto.response.UserProfileResponse;
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
import org.springframework.transaction.support.TransactionTemplate;
import com.digniche.muntum.programreaction.repository.ProgramReactionRepository;
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
    private final ProgramReactionRepository programReactionRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final ProgramRepository programRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final AnnouncementRepository announcementRepository;
    private final CuratorApplicationRepository curatorApplicationRepository;
    private final TermsRepository termsRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final AuthService authService;
    private final SocialAccountRepository socialAccountRepository;
    private final AppleTokenVerifier appleTokenVerifier;
    private final AppleTokenClient appleTokenClient;
    private final SocialTokenCipher socialTokenCipher;
    private final PlatformTransactionManager transactionManager;


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
                .map(user -> UserProfileResponse.from(
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
    //일반 회원 검증 메서드 추가
    private void verifyPasswordWithdrawal(
            User user,
            WithdrawRequest request
    ) {
        if (request.password() == null
                || request.password().isBlank()
                || user.getPassword() == null
                || !passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD
            );
        }
    }
    //apple 재인증 및 철회 메서드 추가
    private void verifySocialWithdrawal(
            SocialAccount socialAccount,
            WithdrawRequest request
    ) {
        if (socialAccount.getProvider()
                != SocialProvider.APPLE) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER
            );
        }

        if (request.token() == null
                || request.token().isBlank()
                || request.authorizationCode() == null
                || request.authorizationCode().isBlank()
                || request.nonce() == null
                || request.nonce().isBlank()) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_REAUTHENTICATION_REQUIRED
            );
        }

        /*
         * 1. 앱에서 직접 받은 Identity Token 검증
         */
        SocialUserInfo requestedUser =
                appleTokenVerifier.verify(
                        new SocialLoginRequest(
                                SocialProvider.APPLE,
                                request.token(),
                                request.authorizationCode(),
                                request.nonce()
                        )
                );

        /*
         * 2. authorizationCode를 Apple 서버와 교환
         */
        AppleTokenResponse tokenResponse =
                appleTokenClient.exchangeAuthorizationCode(
                        request.authorizationCode()
                );

        if (tokenResponse.idToken() == null
                || tokenResponse.idToken().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        /*
         * 3. 교환 결과로 받은 id_token도 다시 검증
         */
        SocialUserInfo exchangedUser =
                appleTokenVerifier.verify(
                        new SocialLoginRequest(
                                SocialProvider.APPLE,
                                tokenResponse.idToken(),
                                null,
                                request.nonce()
                        )
                );

        String savedProviderUserId =
                socialAccount.getProviderUserId();

        /*
         * 세 사용자 식별자가 모두 같아야 함
         *
         * - DB에 저장된 Apple sub
         * - 앱이 보낸 Identity Token의 sub
         * - authorizationCode 교환 결과의 sub
         */
        if (!savedProviderUserId.equals(
                requestedUser.providerUserId()
        )
                || !savedProviderUserId.equals(
                exchangedUser.providerUserId()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        String refreshToken =
                resolveRefreshTokenForRevocation(
                        socialAccount,
                        tokenResponse
                );

        /*
         * DB 삭제 전에 Apple 연결부터 철회
         */
        appleTokenClient.revokeRefreshToken(
                refreshToken
        );
    }
    //철회할 refresh token 선택 메서드 추가
    private String resolveRefreshTokenForRevocation(
            SocialAccount socialAccount,
            AppleTokenResponse tokenResponse
    ) {
        /*
         * 탈퇴 재인증 과정에서 새로 발급된
         * refresh token을 우선 사용
         */
        if (tokenResponse.refreshToken() != null
                && !tokenResponse.refreshToken().isBlank()) {
            return tokenResponse.refreshToken();
        }

        /*
         * 새 토큰이 없다면 로그인 때
         * DB에 저장한 암호문을 복호화
         */
        String encryptedRefreshToken =
                socialAccount.getProviderRefreshToken();

        if (encryptedRefreshToken == null
                || encryptedRefreshToken.isBlank()) {
            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_REVOKE_FAILED
            );
        }

        return socialTokenCipher.decrypt(
                encryptedRefreshToken
        );
    }

    // 회원 탈퇴
    public void withdraw(
            UUID userId,
            WithdrawRequest request,
            String accessToken
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        SocialAccount socialAccount =
                socialAccountRepository.findByUserId(userId)
                        .orElse(null);

        if (socialAccount == null) {
            verifyPasswordWithdrawal(user, request);
        } else {
            verifySocialWithdrawal(
                    socialAccount,
                    request
            );
            log.info(
                    "[APPLE_WITHDRAW_REAUTH_AND_REVOKE_COMPLETED] userId={}",
                    userId
            );
        }

        /*
         * 비밀번호 확인과 Apple 통신이 끝난 뒤
         * 실제 DB 삭제 작업만 트랜잭션으로 실행
         */
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status ->
                completeWithdrawal(userId, accessToken)
        );
        log.info(
                "[WITHDRAW_DB_DELETE_COMPLETED] userId={}",
                userId
        );
    }

    private void completeWithdrawal(UUID userId, String accessToken) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String role = user.getRole().name();
        UUID withdrawnUuid = AuditorAwareImpl.toWithdrawnUserUuid(role, userId);

        switch (role) {
            case "MANAGER" -> {
                // 제보의 생성/수정/검토자/제보자/삭제자 시스템 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.clearReviewedBy(userId);
                spotSuggestionRepository.clearInformer(userId);
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
                // 제보의 생성자/수정자/제보자 시스템 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.clearInformer(userId);

                // 프로그램의 생성자/수정자 Null 처리 및 시스템 UUID 처리
                programRepository.replaceCreatedByWith(userId, withdrawnUuid);
                programRepository.replaceUpdatedByWith(userId, withdrawnUuid);

            }
            case "AUDIENCE" -> {
                // 제보의 생성자/수정자/제보자 Null 처리 및 시스템 UUID 처리
                spotSuggestionRepository.replaceCreatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.replaceUpdatedByWith(userId, withdrawnUuid);
                spotSuggestionRepository.clearInformer(userId);
            }
        }

        // 사용자 관련 데이터 삭제
        userTermsAgreementRepository.deleteAllByUserId(userId);
        scrapRepository.deleteAllByUserId(userId);
        programReactionRepository.deleteAllByUserId(userId);
        userKeywordRepository.deleteAllByUserId(userId);
        curatorApplicationRepository.deleteAllByUserId(userId);

        /*
         * users보다 먼저 삭제해야 함.
         * social_accounts가 users를 외래키로 참조하기 때문임.
         */
        socialAccountRepository.deleteAllByUserId(userId);
        // 탈퇴 후 재가입을 위한 이메일 마스킹 처리(비식별화). DB의 Email 컬럼 Unique 제약 유지.
        //        String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
        //        user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
        //        user.softDelete(userId);

        //        String maskingLetter = MASKING_LETTER_PREFIX + user.getId();
        //        user.maskDeletedUserInfo(maskingLetter, WITHDRAWAL_NICKNAME_PREFIX);
        //        user.softDelete(userId);
        // 현재 Access Token 블랙리스트 등록
        long remainingMillis = authService.calculateTokenTtl(accessToken);

        if (remainingMillis > 0) {
            accessTokenService.addToWithdrawlList(accessToken, remainingMillis);
        }
        refreshTokenService.delete(userId);
        // 회원 탈퇴 N년 후 정보 완전 파기
        //    @Scheduled(cron = "0 0 3 * * *")
        userRepository.delete(user);
    }
}