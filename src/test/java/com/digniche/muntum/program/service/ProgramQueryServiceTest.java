package com.digniche.muntum.program.service;

import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.repository.ProgramQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramQueryServiceTest {

    @Mock private ProgramQueryRepository programQueryRepository;
    @Mock private ProgramService programService;

    @InjectMocks private ProgramQueryService programQueryService;

    @Test
    void 많이_선택된_취향_키워드_목록은_요청의_정렬을_무시하고_페이지_번호와_크기만_넘긴다() {
        Page<Program> empty = Page.empty();
        Page<ProgramCardResponse> emptyCards = Page.empty();
        when(programQueryRepository.findProgramsOrderByMostSelectedTasteKeyword(any())).thenReturn(empty);
        when(programService.toCardResponsePage(empty)).thenReturn(emptyCards);

        programQueryService.getMostSelectedTastePrograms(PageRequest.of(2, 5, Sort.by("title")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(programQueryRepository).findProgramsOrderByMostSelectedTasteKeyword(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }
}
