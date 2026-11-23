package com.digniche.muntum.user.service;

import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.auth.service.AccessTokenService;
import com.digniche.muntum.auth.service.AuthService;
import com.digniche.muntum.curation.repository.CurationImageRepository;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.curator.repository.CuratorApplicationRepository;
import com.digniche.muntum.suggestion.repository.SpotSuggestionRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import com.digniche.muntum.curationlike.service.CurationLikeService;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.search.service.RecentSearchService;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import com.digniche.muntum.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회원 탈퇴 후 Redis 정리 (최근 검색어)
 * - DB 삭제가 커밋된 뒤에 최근 검색어를 지운다
 * - 지우다 실패해도 탈퇴는 성공으로 끝난다 (남은 키는 TTL 30일 뒤 만료)
 * - DB 탈퇴가 실패하면 최근 검색어를 지우지 않는다
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 실패 경로에서는 공통 준비 일부를 쓰지 않는다
class UserServiceWithdrawRedisTest {

    /**
     * 실제 DB 없이 커밋·롤백 흐름과 커밋 후 콜백(afterCommit)만 흉내 내는 트랜잭션 매니저
     */
    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Mock private UserRepository userRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurationLikeService curationLikeService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RecentSearchService recentSearchService;

    // 탈퇴 처리가 부르는 나머지 의존성 (이 테스트에서는 동작만 흘려보낸다)
    @Mock private CurationService curationService;
    @Mock private CurationRepository curationRepository;
    @Mock private CurationImageRepository curationImageRepository;
    @Mock private CuratorApplicationRepository curatorApplicationRepository;
    @Mock private SpotSuggestionRepository spotSuggestionRepository;
    @Mock private UserTermsAgreementRepository userTermsAgreementRepository;
    @Mock private UserProfileImageService userProfileImageService;
    @Mock private AuthService authService;
    @Mock private AccessTokenService accessTokenService;
    @Spy private PlatformTransactionManager transactionManager = new NoOpTransactionManager();

    @InjectMocks private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.AUDIENCE);
        when(user.getPassword()).thenReturn("hashed");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
    }

    @Test
    void 탈퇴가_커밋된_뒤에_최근_검색어를_지운다() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        userService.withdraw(userId, new WithdrawRequest("pw", null, null, null), "access-token");

        InOrder order = inOrder(userRepository, recentSearchService);
        order.verify(userRepository).delete(user);
        order.verify(recentSearchService).deleteAll(userId);
    }

    @Test
    void 최근_검색어_삭제가_실패해도_탈퇴는_성공하고_다음_정리를_계속한다() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("redis down")).when(recentSearchService).deleteAll(userId);

        assertThatCode(() -> userService.withdraw(userId, new WithdrawRequest("pw", null, null, null), "access-token"))
                .doesNotThrowAnyException();

        verify(userRepository).delete(user);
        verify(refreshTokenService).delete(userId);
    }

    @Test
    void DB_탈퇴가_실패하면_최근_검색어를_지우지_않는다() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("db error")).when(curationLikeService).clearLikeActivity(any());

        assertThatThrownBy(() -> userService.withdraw(userId, new WithdrawRequest("pw", null, null, null), "access-token"))
                .hasMessage("db error");

        verify(recentSearchService, never()).deleteAll(any());
    }
}
