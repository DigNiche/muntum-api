package com.digniche.muntum.program.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.repository.ProgramQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로그램 조회 서비스
 * - 목적별(섹션별) 프로그램 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramQueryService {

    private final ProgramQueryRepository programQueryRepository;
    private final ProgramService programService;


    /**
     * 많이 선택된 취향 키워드 순 프로그램 목록 조회
     */
    public PageResponse<ProgramCardResponse> getMostSelectedTastePrograms(Pageable pageable) {
        // 정렬(순서)은 쿼리에서 정해지므로 요청의 Pageable sort는 무시한다
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<Program> programPage = programQueryRepository.findProgramsOrderByMostSelectedTasteKeyword(unsorted);
        return PageResponse.from(programService.toCardResponsePage(programPage));
    }
}
