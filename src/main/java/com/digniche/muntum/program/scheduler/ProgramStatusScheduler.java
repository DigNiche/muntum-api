package com.digniche.muntum.program.scheduler;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramStatusScheduler {

    private final ProgramRepository programRepository;

    /**
     * 매일 새벽 3시에 종료일이 지난 ACTIVE 프로그램을 ENDED로 변경
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updateEndedPrograms() {
        LocalDate today = LocalDate.now();

        List<Program> endedPrograms =
                programRepository.findByStatusAndDeletedAtIsNullAndEndDateBefore(
                        ProgramStatus.ACTIVE,
                        today
                );

        endedPrograms.forEach(program -> program.updateStatus(ProgramStatus.ENDED));

        if (!endedPrograms.isEmpty()) {
            log.info("자동 종료 처리 완료: {}개 프로그램 ACTIVE -> ENDED", endedPrograms.size());
        }
    }
}