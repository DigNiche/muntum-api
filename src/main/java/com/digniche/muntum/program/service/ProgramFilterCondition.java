package com.digniche.muntum.program.service;

import com.digniche.muntum.program.entity.ProgramType;
import java.time.LocalDate;

public record ProgramFilterCondition(
        Boolean freeOnly,
        Boolean noReservationOnly,
        ProgramType programType,
        LocalDate weekStart,
        LocalDate weekEnd
) {}