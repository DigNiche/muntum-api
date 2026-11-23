package com.digniche.muntum.user.service;

import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.global.auditor.SpecialUserType;
import com.digniche.muntum.global.auditor.UserDisplay;
import com.digniche.muntum.global.auditor.UserDisplayLabels;
import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayLookup;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.dto.response.MyCuratorProfileResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuratorProfileService {

    private static final UserDisplayLabels CURATION_AUTHOR_LABELS = new UserDisplayLabels(
            UserDisplayNames.MUNTUM, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.MUNTUM);

    private final CurationRepository curationRepository;
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


    public MyCuratorProfileResponse getMyCuratorProfile(UUID curatorId) {
        CuratorProfileResponse profile = getCurationAuthorProfile(curatorId);

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


    /**
     * 큐레이터 한 명 조회 응답 조립
     */
    private CuratorProfileResponse assembleAuthorProfile(UUID curatorId, UserDisplayLookup displayLookup) {
        UserDisplay display = displayLookup.display(curatorId, CURATION_AUTHOR_LABELS);

        // 현재 존재하는 사용자인 경우
        Optional<User> member = displayLookup.findMember(curatorId);
        if (member.isPresent()) {
            User user = member.get();
            String nickname = user.getRole() == UserRole.MANAGER ? UserDisplayNames.MUNTUM : display.nickname();
            return CuratorProfileResponse.from(user.getId(), user.getRole().name(), nickname, user.getProfileImageUrl());
        }

        // 존재하지 않는 사용자/시스템인 경우
        String role = SpecialUserType.from(curatorId).map(type -> type.role().name()).orElse(null);
        return CuratorProfileResponse.from(display.id(), role, display.nickname(), null);
    }
}
