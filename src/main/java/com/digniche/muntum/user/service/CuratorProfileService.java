package com.digniche.muntum.user.service;

import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.global.auditor.SpecialUserType;
import com.digniche.muntum.global.auditor.UserDisplay;
import com.digniche.muntum.global.auditor.UserDisplayLabels;
import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayLookup;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.dto.response.MyCuratorProfileResponse;
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
import java.util.Optional;
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
    private static final UserDisplayLabels CURATION_AUTHOR_LABELS = new UserDisplayLabels(
            UserDisplayNames.MUNTUM, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.MUNTUM);

    private final CurationRepository curationRepository;
    private final UserRepository userRepository;
    private final UserDisplayResolver userDisplayResolver;


    /**
     * 큐레이터 한 명 조회
     */
    public CuratorProfileResponse getCurationAuthorProfile(UUID curatorId) {
        return getCurationAuthorProfiles(List.of(curatorId)).get(curatorId);
    }

    /**
     * 큐레이터 여러 명 일괄 조회
     */
    public Map<UUID, CuratorProfileResponse> getCurationAuthorProfiles(Collection<UUID> curatorIds) {
        if (curatorIds == null || curatorIds.isEmpty()) { return Map.of(); }

        UserDisplayLookup displayLookup = userDisplayResolver.load(curatorIds);

        Map<UUID, CuratorProfileResponse> result = new LinkedHashMap<>();
        for (UUID curatorId : new LinkedHashSet<>(curatorIds)) {
            result.put(curatorId, assembleAuthorProfile(curatorId, displayLookup));
        }
        return result;
    }

    /**
     * 큐레이터 한 명 조회
     */
    public CuratorProfileResponse getCuratorProfile(UUID curatorId) {
        return getCuratorProfiles(List.of(curatorId))
                .get(curatorId);
    }

    /**
     * 목록 응답을 위한 큐레이터 일괄 조회
     */
    public Map<UUID, CuratorProfileResponse> getCuratorProfiles(Collection<UUID> curatorIds) {
        if (curatorIds == null || curatorIds.isEmpty()) { return Map.of();}

        Set<UUID> uniqueIds = new LinkedHashSet<>(curatorIds);
        List<UUID> activeUserIds = uniqueIds.stream().filter(
                                    id -> !isWithdrawnManager(id) && !isWithdrawnCurator(id)
                                    ).toList();

        Map<UUID, User> userMap = userRepository.findAllById(activeUserIds).stream()
                                    .collect(
                                            Collectors.toMap(User::getId, Function.identity())
                                    );

        Map<UUID, CuratorProfileResponse> result = new LinkedHashMap<>();

        for (UUID curatorId : uniqueIds) {
            result.put(curatorId, createProfile(curatorId, userMap));
        }

        return result;
    }


    public MyCuratorProfileResponse getMyCuratorProfile(UUID curatorId) {
        CuratorProfileResponse profile = getCuratorProfile(curatorId);

        long approvedCount = curationRepository.countByCuratorIdAndStatusAndDeletedAtIsNull(curatorId, CurationStatus.APPROVED);

        long pendingCount = curationRepository.countByCuratorIdAndStatusAndDeletedAtIsNull(curatorId, CurationStatus.PENDING);

        long changesRequestedCount = curationRepository.countByCuratorIdAndStatusAndDeletedAtIsNull(curatorId, CurationStatus.CHANGES_REQUESTED);

        return new MyCuratorProfileResponse(
                profile.curatorId(), profile.role(), profile.nickname(),
                profile.profileImageUrl(),
                approvedCount, pendingCount,
                changesRequestedCount
        );
    }


    private CuratorProfileResponse createProfile(UUID curatorId, Map<UUID, User> userMap) {
        if (isWithdrawnManager(curatorId)) {
            return CuratorProfileResponse.from(curatorId, UserRole.MANAGER.name(), MANAGER_NICKNAME, null);
        }

        if (isWithdrawnCurator(curatorId)) {
            return CuratorProfileResponse.from(curatorId, UserRole.CURATOR.name(), WITHDRAWN_CURATOR_NICKNAME,null);
        }

        User user = userMap.get(curatorId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String nickname = user.getRole() == UserRole.MANAGER ? MANAGER_NICKNAME : user.getNickname();

        return CuratorProfileResponse.from(user.getId(), user.getRole().name(), nickname, user.getProfileImageUrl());
    }


    /**
     * 큐레이터 한 명 조회 응답 조립
     */
    private CuratorProfileResponse assembleAuthorProfile(UUID curatorId, UserDisplayLookup displayLookup) {
        UserDisplay display = displayLookup.display(curatorId, CURATION_AUTHOR_LABELS);

        // 현재 존재하는 사용자인 경우
        Optional<User> member = displayLookup.findMember(curatorId);
        if (member.isPresent()) {
            User user = member.get();
            String nickname = user.getRole() == UserRole.MANAGER ? MANAGER_NICKNAME : display.nickname();
            return CuratorProfileResponse.from(user.getId(), user.getRole().name(), nickname, user.getProfileImageUrl());
        }

        // 존재하지 않는 사용자/시스템인 경우
        String role = SpecialUserType.from(curatorId).map(CuratorProfileService::roleOf).orElse(null);
        return CuratorProfileResponse.from(display.id(), role, display.nickname(), null);
    }

    /**
     * 특수 UUID 종류 별 역할 응답
     */
    private static String roleOf(SpecialUserType type) {
        return switch (type) {
            case WITHDRAWN_MANAGER, SYSTEM -> UserRole.MANAGER.name();
            case WITHDRAWN_CURATOR -> UserRole.CURATOR.name();
            case WITHDRAWN_AUDIENCE -> UserRole.AUDIENCE.name();
        };
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