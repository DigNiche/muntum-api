package com.digniche.muntum.global.auditor;

import com.digniche.muntum.user.entity.UserRole;

import java.util.Set;

/**
 * 탈퇴자/시스템/알 수 없는 사용자를 화면에 표시할 때 쓰는 문구
 * - 칸별 문구표(UserDisplayLabels)는 이 상수로 만든다
 */
public final class UserDisplayNames {

    public static final String MUNTUM = "문틈";
    public static final String ANONYMOUS_CURATOR = "익명의 큐레이터";
    public static final String ANONYMOUS_AUDIENCE = "익명의 관람객";
    public static final String ANONYMOUS_MANAGER = "익명의 관리자";
    public static final String ANONYMOUS_INFORMER = "익명의 제보자";
    public static final String UNKNOWN_USER = "알 수 없음";

    /**
     * 닉네임 예약어 (서비스 이름): 관리자만 쓸 수 있다
     */
    public static final Set<String> SERVICE_NAME_NICKNAMES = Set.of(MUNTUM, "Muntum", "MUNTUM", "muntum");

    /**
     * 닉네임 예약어 (탈퇴자·알 수 없음 표시 문구): 아무도 쓸 수 없다
     */
    public static final Set<String> ANONYMOUS_NICKNAMES = Set.of(
            ANONYMOUS_CURATOR, ANONYMOUS_AUDIENCE, ANONYMOUS_MANAGER, ANONYMOUS_INFORMER, UNKNOWN_USER);

    /**
     * 이 역할이 쓸 수 없는 예약어면 true. 앞뒤 공백만 빼고 예약어와 똑같을 때만 막는다
     */
    public static boolean isReservedNicknameFor(String nickname, UserRole role) {
        if (nickname == null) {
            return false;
        }
        String stripped = nickname.strip();
        if (ANONYMOUS_NICKNAMES.contains(stripped)) {
            return true;
        }
        return role != UserRole.MANAGER && SERVICE_NAME_NICKNAMES.contains(stripped);
    }

    private UserDisplayNames() {
    }
}
