package com.digniche.muntum.suggestion.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.suggestion.dto.request.SpotSuggestionRequest;
import com.digniche.muntum.suggestion.dto.request.SuggestionStatusUpdateRequest;
import com.digniche.muntum.suggestion.dto.response.SpotSuggestionResponse;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import com.digniche.muntum.suggestion.service.SpotSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 프로그램 제보 컨트롤러
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/suggestions") //
public class SpotSuggestionController {

    private final SpotSuggestionService spotSuggestionService;

    // 제보 등록
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<ApiResponse<SpotSuggestionResponse>> suggestProgramSpot(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SpotSuggestionRequest request
    ) {
        SpotSuggestionResponse response =
                spotSuggestionService.createSpotSuggestion(userPrincipal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("제보가 등록되었습니다.", response));
    }

    // 내 제보 목록 조회
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<SpotSuggestionResponse>>> getMySuggestions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<SpotSuggestionResponse> response =
                spotSuggestionService.getMySuggestionList(userPrincipal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("내 제보 목록 조회에 성공했습니다.", response));
    }

    // 전체 제보 목록 조회 - 상태 필터 선택적
    @PreAuthorize("hasAnyRole('MANAGER')")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<PageResponse<SpotSuggestionResponse>>> getSpotSuggestions(
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<SpotSuggestionResponse> response =
                spotSuggestionService.getSpotSuggestionList(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("제보 전체 목록 조회에 성공했습니다.", response));
    }

    /**
     * 제보 단건 조회 - 작성자 본인 또는 관리자
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{suggestion_id}")
    public ResponseEntity<ApiResponse<SpotSuggestionResponse>> getSpotSuggestion(
            @PathVariable("suggestion_id") UUID suggestionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        SpotSuggestionResponse response = spotSuggestionService.getSpotSuggestion(
                suggestionId, userPrincipal.getUserId(), userPrincipal.getUserRole());
        return ResponseEntity.ok(ApiResponse.success("제보 조회에 성공했습니다.", response));
    }


    // 제보 수정 - 작성자 본인, PENDING 상태에 한함 (서비스에서 검증)
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{suggestion_id}")
    public ResponseEntity<ApiResponse<SpotSuggestionResponse>> updateSpotSuggestion(
            @PathVariable("suggestion_id") UUID suggestionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SpotSuggestionRequest request
    ) {
        SpotSuggestionResponse response = spotSuggestionService.updateSpotSuggestion(
                suggestionId, userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("제보가 수정되었습니다.", response));
    }

    // 제보 상태 변경 - 관리자 전용
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PatchMapping("/{suggestion_id}/status")
    public ResponseEntity<ApiResponse<SpotSuggestionResponse>> updateSuggestionStatus(
            @PathVariable("suggestion_id") UUID suggestionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SuggestionStatusUpdateRequest request
    ) {
        SpotSuggestionResponse response = spotSuggestionService.updateSuggestionStatus(
                suggestionId, userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("제보 상태가 변경되었습니다.", response));
    }

    //제보 삭제 - 관리자 전용, 완전 삭제
    @PreAuthorize("hasAnyRole('MANAGER')")
    @DeleteMapping("/{suggestion_id}")
    public ResponseEntity<ApiResponse<Void>> deleteSpotSuggestion(
            @PathVariable("suggestion_id") UUID suggestionId
    ) {
        spotSuggestionService.deleteSpotSuggestion(suggestionId);
        return ResponseEntity.ok(ApiResponse.success("제보가 삭제되었습니다.", null));
    }
}