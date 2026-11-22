package com.digniche.muntum.global.auditor;

import com.digniche.muntum.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 닉네임 예약어 판별: 앞뒤 공백만 빼고 예약어와 똑같을 때만 막는다
 * - 서비스 이름(문틈, Muntum, MUNTUM, muntum): 관리자만 허용
 * - 탈퇴자·알 수 없음 표시 문구: 모든 역할 차단
 */
class UserDisplayNamesTest {

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void 탈퇴자_알_수_없음_문구는_모든_역할이_쓸_수_없다(UserRole role) {
        for (String nickname : new String[]{"익명의 큐레이터", "익명의 관람객", "익명의 관리자", "익명의 제보자", "알 수 없음"}) {
            assertThat(UserDisplayNames.isReservedNicknameFor(nickname, role)).as(nickname).isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CURATOR", "AUDIENCE"})
    void 서비스_이름은_큐레이터와_관람객이_쓸_수_없다(UserRole role) {
        for (String nickname : new String[]{"문틈", "Muntum", "MUNTUM", "muntum"}) {
            assertThat(UserDisplayNames.isReservedNicknameFor(nickname, role)).as(nickname).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"문틈", "Muntum", "MUNTUM", "muntum"})
    void 서비스_이름은_관리자가_쓸_수_있다(String nickname) {
        assertThat(UserDisplayNames.isReservedNicknameFor(nickname, UserRole.MANAGER)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {" 문틈 ", "문틈\t", "　익명의 큐레이터", " muntum "})
    void 앞뒤_공백은_빼고_비교한다(String nickname) {
        assertThat(UserDisplayNames.isReservedNicknameFor(nickname, UserRole.CURATOR)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "문 틈", "익명의큐레이터", "알수없음",
            "MunTum", "mUNTUM", "Mun tum", "muntum1",
            "문틈큐레이터", "문틈 큐레이터", "익명의 큐레이터님", "민정"
    })
    void 가운데_공백이나_글자가_다르면_허용한다(String nickname) {
        assertThat(UserDisplayNames.isReservedNicknameFor(nickname, UserRole.CURATOR)).isFalse();
    }

    @Test
    void null은_예약어가_아니다() {
        assertThat(UserDisplayNames.isReservedNicknameFor(null, UserRole.CURATOR)).isFalse();
    }
}
