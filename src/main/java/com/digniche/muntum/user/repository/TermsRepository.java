package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 약관 데이터 접근 계층
 */
public interface TermsRepository extends JpaRepository<Terms, UUID> {

    // 단건 조회 (삭제 제외)
    Optional<Terms> findByIdAndDeletedAtIsNull(UUID id);

    // 현재 게시 중인 약관 전체 (사용자용 목록)
    List<Terms> findAllByActiveTrueAndDeletedAtIsNull();

    // 타입별 현재 활성 버전 (활성화 시 기존 버전 내리기용)
    Optional<Terms> findByTypeAndActiveTrueAndDeletedAtIsNull(UserTermsType type);

    // 같은 타입 + 같은 버전 중복 등록 방지
    boolean existsByTypeAndVersionAndDeletedAtIsNull(UserTermsType type, String version);

    // 타입별 전체 이력 (관리자용, 최신 시행일 순)
    List<Terms> findAllByTypeAndDeletedAtIsNullOrderByEffectiveAtDesc(UserTermsType type);

    /**
     * 사용자 삭제 시
     * - 생성자, 수정자, 삭제자 System UUID로 채우기
     */

    @Modifying
    @Query("UPDATE Terms t SET t.createdBy = :systemUuid WHERE t.createdBy = :userId")
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Terms t SET t.updatedBy = NULL WHERE t.updatedBy = :userId")
    void replaceUpdatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Terms t SET t.deletedBy = :systemUuid WHERE t.deletedBy = :userId")
    void replaceDeletedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);
}
