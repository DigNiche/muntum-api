package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curationlike.repository.CurationLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 큐레이션 좋아요 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationLikeQueryService {

    private final CurationLikeRepository curationLikeRepository;

    /**
     * 큐레이션별 좋아요 집계 정보/내 상태 정보
     */
    public Map<UUID, CurationLikeResponse> getLikes(Collection<Curation> curations, UUID userId) {
        Set<UUID> likedIds = findLikedCurationIdsForUser(curations, userId);

        return curations.stream().collect(Collectors.toMap(
                                            Curation::getId,
                                    curation -> CurationLikeResponse.of(curation, likedIds.contains(curation.getId())),
                                            (first, second) -> first)
                                        );
    }


    /**
     * 큐레이션 한 건의 좋아요 집계 정보/내 상태 정보
     */
    public CurationLikeResponse getLike(Curation curation, UUID userId) {
        return getLikes(List.of(curation), userId).get(curation.getId());
    }


    /**
     * 사용자가 좋아요한 큐레이션의 ID 목록 조회
     */
    private Set<UUID> findLikedCurationIdsForUser(Collection<Curation> curations, UUID userId) {
        if (userId == null || curations.isEmpty()) {
            return Set.of();
        }
        List<UUID> curationIds = curations.stream().map(Curation::getId).distinct().toList();

        return new HashSet<>(curationLikeRepository.findLikedCurationIds(userId, curationIds));
    }
}
