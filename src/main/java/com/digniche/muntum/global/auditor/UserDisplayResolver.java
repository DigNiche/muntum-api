package com.digniche.muntum.global.auditor;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 작성자 칸(created_by, reviewed_by 등)의 UUID를 화면에 보여 줄 값으로 바꾸는 공통 조회
 *
 * 1. load: 화면에 나올 모든 칸의 UUID를 한 번에 받아 회원을 한 번만 조회한다
 *    - Null은 제외
 *    - 특수 UUID(탈퇴자/시스템)는 DB에 묻지 않는다
 *    - 남은 일반 회원 UUID만 모아 한 번에 조회
 *    - User DB에 없는 일반 UUID는 경고 로그를 남긴다
 * 2. UserDisplayLookup.display: 칸마다 자기 문구표로 표시 값을 꺼낸다 (DB 조회 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDisplayResolver {

    private final UserRepository userRepository;

    /**
     * 사용자 칸 UUID들 중 일반 회원만 한 번에 조회해, 칸별 표시 값을 꺼낼 UserDisplayLookup으로 돌려준다
     */
    public UserDisplayLookup load(Collection<UUID> ids) {
        Set<UUID> memberIds = ids.stream()
                .filter(Objects::nonNull).filter(id -> SpecialUserType.from(id).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (memberIds.isEmpty()) { return new UserDisplayLookup(Map.of()); }

        Map<UUID, User> members = userRepository.findAllById(memberIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        memberIds.stream().filter(id -> !members.containsKey(id))
                .forEach(id -> log.warn(
                        "[USER_DISPLAY_UNKNOWN] users에 없는 일반 UUID - 탈퇴 처리가 빠진 칸인지 확인 필요: {}", id));

        return new UserDisplayLookup(members);
    }
}
