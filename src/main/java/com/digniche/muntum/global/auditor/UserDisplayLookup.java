package com.digniche.muntum.global.auditor;

import com.digniche.muntum.user.entity.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UserDisplayResolver.load로 한 번 조회한 회원 결과를 담아 두고, 칸마다 자기 문구표로 표시 값을 꺼낸다
 */
public class UserDisplayLookup {

    private final Map<UUID, User> members;

    UserDisplayLookup(Map<UUID, User> members) {
        this.members = members;
    }

    /**
     * 사용자의 ID/이름 디스플레이할 때
     * @param id     작성자 칸 UUID (load에 넘긴 id 중 하나)
     * @param labels 이 칸의 문구표
     * @return 표시 값. id가 null이면 null
     */
    public UserDisplay display(UUID id, UserDisplayLabels labels) {
        if (id == null) {
            return null;
        }
        // 특수 UUID(탈퇴자/시스템): 문구표의 문구
        Optional<SpecialUserType> special = SpecialUserType.from(id);
        if (special.isPresent()) {
            return new UserDisplay(null, labels.labelOf(special.get()));
        }
        // 일반 회원: 닉네임
        User member = members.get(id);
        if (member != null) {
            return new UserDisplay(member.getId(), member.getNickname());
        }
        // users에 없는 일반 UUID (경고 로그는 load에서 남김)
        return new UserDisplay(null, UserDisplayNames.UNKNOWN_USER);
    }

    /**
     * 사용자의 ID/이름 외 사용자 정보 디스플레이할 때
     */
    public Optional<User> findMember(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.get(id));
    }
}
