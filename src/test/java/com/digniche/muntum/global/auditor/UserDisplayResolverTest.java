package com.digniche.muntum.global.auditor;

import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 작성자 칸 UUID → 화면 표시 값 공통 조회
 * - load: 회원 조회 한 번
 * - display: 칸마다 자기 문구표로 표시 값 (DB 조회 없음)
 */
class UserDisplayResolverTest {

    private static final UserDisplayLabels LABELS_A =
            new UserDisplayLabels("A 탈퇴 관리자", "A 탈퇴 큐레이터", "A 탈퇴 관람객", "A 시스템");
    private static final UserDisplayLabels LABELS_B =
            new UserDisplayLabels("B 탈퇴자", "B 탈퇴자", "B 탈퇴자", "B 시스템");

    private UserRepository userRepository;
    private UserDisplayResolver resolver;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        resolver = new UserDisplayResolver(userRepository);
    }

    @Test
    void 특수_UUID는_DB를_조회하지_않고_문구표의_문구로_바뀐다() {
        UUID original = UUID.randomUUID();
        UUID manager = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", original);
        UUID curator = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", original);
        UUID audience = AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", original);
        UUID system = AuditorAwareImpl.SYSTEM_UUID;

        UserDisplayLookup lookup = resolver.load(List.of(manager, curator, audience, system));

        assertThat(lookup.display(manager, LABELS_A)).isEqualTo(new UserDisplay(null, "A 탈퇴 관리자"));
        assertThat(lookup.display(curator, LABELS_A)).isEqualTo(new UserDisplay(null, "A 탈퇴 큐레이터"));
        assertThat(lookup.display(audience, LABELS_A)).isEqualTo(new UserDisplay(null, "A 탈퇴 관람객"));
        assertThat(lookup.display(system, LABELS_A)).isEqualTo(new UserDisplay(null, "A 시스템"));
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void null만_있으면_DB를_조회하지_않고_null로_나온다() {
        UserDisplayLookup lookup = resolver.load(Arrays.asList(null, null));

        assertThat(lookup.display(null, LABELS_A)).isNull();
        verify(userRepository, never()).findAllById(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void 일반_회원_UUID는_중복을_빼고_한_번에_조회해_닉네임으로_바뀐다() {
        User minjung = member("민정");
        User nayeon = member("나연");
        when(userRepository.findAllById(any())).thenReturn(List.of(minjung, nayeon));

        UserDisplayLookup lookup = resolver.load(
                List.of(minjung.getId(), nayeon.getId(), minjung.getId(), AuditorAwareImpl.SYSTEM_UUID));

        assertThat(lookup.display(minjung.getId(), LABELS_A)).isEqualTo(new UserDisplay(minjung.getId(), "민정"));
        assertThat(lookup.display(nayeon.getId(), LABELS_A)).isEqualTo(new UserDisplay(nayeon.getId(), "나연"));

        ArgumentCaptor<Collection<UUID>> queried = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository, times(1)).findAllById(queried.capture());
        assertThat(queried.getValue()).containsExactlyInAnyOrder(minjung.getId(), nayeon.getId());
    }

    @Test
    void 한_번_조회한_결과에서_칸마다_다른_문구표를_쓸_수_있다() {
        User minjung = member("민정");
        UUID withdrawnManager = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", UUID.randomUUID());
        UUID withdrawnAudience = AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", UUID.randomUUID());
        when(userRepository.findAllById(any())).thenReturn(List.of(minjung));

        UserDisplayLookup lookup = resolver.load(List.of(minjung.getId(), withdrawnManager, withdrawnAudience));

        // 같은 특수 UUID라도 칸(문구표)에 따라 다른 문구
        assertThat(lookup.display(withdrawnManager, LABELS_A).nickname()).isEqualTo("A 탈퇴 관리자");
        assertThat(lookup.display(withdrawnManager, LABELS_B).nickname()).isEqualTo("B 탈퇴자");
        assertThat(lookup.display(withdrawnAudience, LABELS_B).nickname()).isEqualTo("B 탈퇴자");
        // 일반 회원은 문구표와 상관없이 닉네임
        assertThat(lookup.display(minjung.getId(), LABELS_B)).isEqualTo(new UserDisplay(minjung.getId(), "민정"));
        verify(userRepository, times(1)).findAllById(any());
    }

    @Test
    void users에_없는_일반_UUID는_알_수_없음으로_바뀐다() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findAllById(any())).thenReturn(List.of());

        UserDisplayLookup lookup = resolver.load(List.of(missing));

        assertThat(lookup.display(missing, LABELS_A))
                .isEqualTo(new UserDisplay(null, UserDisplayNames.UNKNOWN_USER));
    }

    @Test
    void findMember는_현재_회원만_돌려주고_DB를_다시_조회하지_않는다() {
        User minjung = member("민정");
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", UUID.randomUUID());
        UUID missing = UUID.randomUUID();
        when(userRepository.findAllById(any())).thenReturn(List.of(minjung));

        UserDisplayLookup lookup = resolver.load(Arrays.asList(minjung.getId(), withdrawn, missing, null));

        assertThat(lookup.findMember(minjung.getId())).contains(minjung);
        assertThat(lookup.findMember(withdrawn)).isEmpty();
        assertThat(lookup.findMember(missing)).isEmpty();
        assertThat(lookup.findMember(null)).isEmpty();
        verify(userRepository, times(1)).findAllById(any());
    }

    private static User member(String nickname) {
        User user = mock(User.class);
        UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        when(user.getNickname()).thenReturn(nickname);
        return user;
    }
}
