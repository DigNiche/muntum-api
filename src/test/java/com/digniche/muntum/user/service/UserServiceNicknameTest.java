package com.digniche.muntum.user.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.request.NicknameUpdateRequest;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 닉네임 설정 예약어 검사
 * - 큐레이터·관람객은 예약어 불가, 관리자는 서비스 이름만 허용
 * - 검사 순서: 예약어 → 중복
 */
@ExtendWith(MockitoExtension.class)
class UserServiceNicknameTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private final UUID userId = UUID.randomUUID();

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CURATOR", "AUDIENCE"})
    void 큐레이터와_관람객은_예약어로_바꿀_수_없고_중복_검사까지_가지_않는다(UserRole role) {
        User user = givenUser(role);

        assertThatThrownBy(() -> userService.setNickname(userId, new NicknameUpdateRequest("문틈")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESERVED_NICKNAME);

        verify(userRepository, never()).existsByNicknameAndIdNot(anyString(), any());
        assertThat(user.getNickname()).isNull();
    }

    @Test
    void 관리자는_예약어로_바꿀_수_있다() {
        User user = givenUser(UserRole.MANAGER);
        when(userRepository.existsByNicknameAndIdNot("문틈", userId)).thenReturn(false);

        userService.setNickname(userId, new NicknameUpdateRequest("문틈"));

        assertThat(user.getNickname()).isEqualTo("문틈");
    }

    @Test
    void 관리자도_탈퇴자_표시_문구로는_바꿀_수_없다() {
        User user = givenUser(UserRole.MANAGER);

        assertThatThrownBy(() -> userService.setNickname(userId, new NicknameUpdateRequest("익명의 큐레이터")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESERVED_NICKNAME);

        verify(userRepository, never()).existsByNicknameAndIdNot(anyString(), any());
        assertThat(user.getNickname()).isNull();
    }

    @Test
    void 관리자도_이미_쓰는_예약어면_중복으로_막힌다() {
        givenUser(UserRole.MANAGER);
        when(userRepository.existsByNicknameAndIdNot("문틈", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.setNickname(userId, new NicknameUpdateRequest("문틈")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    @Test
    void 예약어가_아니면_큐레이터도_바꿀_수_있다() {
        User user = givenUser(UserRole.CURATOR);
        when(userRepository.existsByNicknameAndIdNot("문 틈", userId)).thenReturn(false);

        userService.setNickname(userId, new NicknameUpdateRequest("문 틈"));

        assertThat(user.getNickname()).isEqualTo("문 틈");
    }

    private User givenUser(UserRole role) {
        User user = User.builder().email("user@test.com").role(role).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        return user;
    }
}
