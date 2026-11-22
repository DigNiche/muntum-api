package com.digniche.muntum.user.service;

import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 큐레이션 작성자 카드 (getCurationAuthorProfile(s))
 * - 현재 회원: 실제 id·역할·닉네임(관리자는 "문틈")·프로필 이미지
 * - 탈퇴자·시스템: id·이미지 null, 문구표 (관리자·시스템 "문틈", 큐레이터·관람객 "익명의 큐레이터")
 * - 알 수 없음: 화면 실패 대신 "알 수 없음", 역할 null
 */
class CuratorProfileServiceAuthorProfileTest {

    private UserRepository userRepository;
    private CuratorProfileService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CuratorProfileService(mock(CurationRepository.class), userRepository, new UserDisplayResolver(userRepository));
    }

    @Test
    void 현재_큐레이터는_본인_닉네임과_프로필_이미지로_나온다() {
        User curator = member(UserRole.CURATOR, "민정", "https://storage/minjung.jpg");
        when(userRepository.findAllById(any())).thenReturn(List.of(curator));

        CuratorProfileResponse profile = service.getCurationAuthorProfile(curator.getId());

        assertThat(profile).isEqualTo(new CuratorProfileResponse(curator.getId(), "CURATOR", "민정", "https://storage/minjung.jpg"));
    }

    @Test
    void 현재_관리자는_닉네임_대신_문틈으로_나오고_프로필_이미지는_본인_것이다() {
        User manager = member(UserRole.MANAGER, "운영자A", "https://storage/manager.jpg");
        when(userRepository.findAllById(any())).thenReturn(List.of(manager));

        CuratorProfileResponse profile = service.getCurationAuthorProfile(manager.getId());

        assertThat(profile).isEqualTo(new CuratorProfileResponse(manager.getId(), "MANAGER", UserDisplayNames.MUNTUM, "https://storage/manager.jpg"));
    }

    @Test
    void 탈퇴자와_시스템은_DB를_조회하지_않고_id와_이미지_없이_문구표로_나온다() {
        UUID withdrawnCurator = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", UUID.randomUUID());
        UUID withdrawnManager = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", UUID.randomUUID());
        UUID withdrawnAudience = AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", UUID.randomUUID());
        UUID system = AuditorAwareImpl.SYSTEM_UUID;

        Map<UUID, CuratorProfileResponse> profiles = service.getCurationAuthorProfiles(
                List.of(withdrawnCurator, withdrawnManager, withdrawnAudience, system));

        assertThat(profiles.get(withdrawnCurator)).isEqualTo(new CuratorProfileResponse(null, "CURATOR", UserDisplayNames.ANONYMOUS_CURATOR, null));
        assertThat(profiles.get(withdrawnManager)).isEqualTo(new CuratorProfileResponse(null, "MANAGER", UserDisplayNames.MUNTUM, null));
        assertThat(profiles.get(withdrawnAudience)).isEqualTo(new CuratorProfileResponse(null, "AUDIENCE", UserDisplayNames.ANONYMOUS_CURATOR, null));
        assertThat(profiles.get(system)).isEqualTo(new CuratorProfileResponse(null, "MANAGER", UserDisplayNames.MUNTUM, null));
        verify(userRepository, times(0)).findAllById(any());
    }

    @Test
    void users에_없는_일반_UUID는_화면을_실패시키지_않고_알_수_없음으로_나온다() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findAllById(any())).thenReturn(List.of());

        CuratorProfileResponse profile = service.getCurationAuthorProfile(missing);

        assertThat(profile).isEqualTo(new CuratorProfileResponse(null, null, UserDisplayNames.UNKNOWN_USER, null));
    }

    @Test
    void 목록은_중복을_빼고_회원을_한_번만_조회하며_키는_넘겨받은_작성자_UUID다() {
        User curator = member(UserRole.CURATOR, "민정", null);
        UUID withdrawnCurator = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", UUID.randomUUID());
        when(userRepository.findAllById(any())).thenReturn(List.of(curator));

        Map<UUID, CuratorProfileResponse> profiles = service.getCurationAuthorProfiles(
                List.of(curator.getId(), withdrawnCurator, curator.getId()));

        assertThat(profiles).containsOnlyKeys(curator.getId(), withdrawnCurator);
        assertThat(profiles.get(curator.getId()).nickname()).isEqualTo("민정");
        assertThat(profiles.get(withdrawnCurator).curatorId()).isNull();
        verify(userRepository, times(1)).findAllById(any());
    }

    private static User member(UserRole role, String nickname, String profileImageUrl) {
        User user = mock(User.class);
        UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.getNickname()).thenReturn(nickname);
        when(user.getProfileImageUrl()).thenReturn(profileImageUrl);
        return user;
    }
}
