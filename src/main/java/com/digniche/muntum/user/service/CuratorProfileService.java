package com.digniche.muntum.user.service;

import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuratorProfileService {

    private static final String MANAGER_NICKNAME = "문틈";
    private static final String WITHDRAWN_CURATOR_NICKNAME = "익명의 큐레이터";

    private final UserRepository userRepository;

    /**
     * 큐레이터 한 명 조회
     */
    public CuratorProfileResponse getCuratorProfile(
            UUID curatorId
    ) {
        return getCuratorProfiles(List.of(curatorId))
                .get(curatorId);
    }

    /**
     * 목록 응답을 위한 큐레이터 일괄 조회
     */
    public Map<UUID, CuratorProfileResponse>
    getCuratorProfiles(
            Collection<UUID> curatorIds
    ) {
        if (curatorIds == null || curatorIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> uniqueIds =
                new LinkedHashSet<>(curatorIds);

        List<UUID> activeUserIds =
                uniqueIds.stream()
                        .filter(id ->
                                !isWithdrawnManager(id)
                                        && !isWithdrawnCurator(id)
                        )
                        .toList();

        Map<UUID, User> userMap =
                userRepository.findAllById(activeUserIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        Function.identity()
                                )
                        );

        Map<UUID, CuratorProfileResponse> result =
                new LinkedHashMap<>();

        for (UUID curatorId : uniqueIds) {
            result.put(
                    curatorId,
                    createProfile(
                            curatorId,
                            userMap
                    )
            );
        }

        return result;
    }

    private CuratorProfileResponse createProfile(
            UUID curatorId,
            Map<UUID, User> userMap
    ) {
        if (isWithdrawnManager(curatorId)) {
            return CuratorProfileResponse.from(
                    curatorId,
                    UserRole.MANAGER.name(),
                    MANAGER_NICKNAME,
                    null
            );
        }

        if (isWithdrawnCurator(curatorId)) {
            return CuratorProfileResponse.from(
                    curatorId,
                    UserRole.CURATOR.name(),
                    WITHDRAWN_CURATOR_NICKNAME,
                    null
            );
        }

        User user =
                userMap.get(curatorId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND
            );
        }

        String nickname =
                user.getRole() == UserRole.MANAGER
                        ? MANAGER_NICKNAME
                        : user.getNickname();

        return CuratorProfileResponse.from(
                user.getId(),
                user.getRole().name(),
                nickname,
                user.getProfileImageUrl()
        );
    }

    private boolean isWithdrawnManager(UUID userId) {
        return userId.toString().startsWith(
                AuditorAwareImpl
                        .WITHDRAWN_MANAGER_UUID_PREFIX
        );
    }

    private boolean isWithdrawnCurator(UUID userId) {
        return userId.toString().startsWith(
                AuditorAwareImpl
                        .WITHDRAWN_CURATOR_UUID_PREFIX
        );
    }
}