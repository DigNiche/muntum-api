package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.program.entity.ProgramStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좋아요 불가 이유 분류(CurationLikeService.resolveCurationNotLikeableErrorCode)가 모든 상태값을 다루는지 확인
 * - 분류에 없는 상태값은 마지막 분기(CUL004, 다시 시도)로 내려가는데, 다시 보내도 성공하지 않는다
 * - 상태값이 추가되면 이 테스트가 깨진다. 분류에 분기를 추가한 뒤 아래 목록을 고친다
 */
class CurationLikeStatusCoverageTest {

    @Test
    void 프로그램_상태값은_모두_공개_기준이거나_좋아요_불가_분류에_들어간다() {
        Set<ProgramStatus> classified = new HashSet<>(ProgramStatus.PUBLIC_VIEWABLE);
        classified.addAll(List.of(ProgramStatus.DELETED, ProgramStatus.HIDDEN)); // CUL002, CUL003

        assertThat(ProgramStatus.values())
                .as("분류되지 않은 ProgramStatus가 있다. resolveCurationNotLikeableErrorCode에 분기를 추가한다")
                .allMatch(classified::contains);
    }

    @Test
    void 큐레이션_공개_상태값은_모두_공개_기준이거나_좋아요_불가_분류에_들어간다() {
        Set<CurationPublicationStatus> classified = Set.of(
                CurationPublicationStatus.PUBLISHED,   // 공개 기준
                CurationPublicationStatus.UNPUBLISHED  // CUL001
        );

        assertThat(CurationPublicationStatus.values())
                .as("분류되지 않은 CurationPublicationStatus가 있다. resolveCurationNotLikeableErrorCode에 분기를 추가한다")
                .allMatch(classified::contains);
    }
}
