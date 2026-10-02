package com.digniche.muntum.program.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로그램 예약 방식·예약 링크 저장 및 수정
 * - 수정은 다른 필드와 달리 항상 요청 값으로 덮어씀 : null이면 지움, 값이 있으면 변경
 * - 기존 값을 유지하려면 기존 값을 그대로 담아 보내야 함
 */
class ProgramReservationInfoTest {

    @Test
    void 등록_시_예약_방식과_예약_링크를_저장한다() {
        Program program = program(ReservationType.PRE_REGISTRATION, "https://booking.example.com");

        assertThat(program.getReservationType()).isEqualTo(ReservationType.PRE_REGISTRATION);
        assertThat(program.getReservationUrl()).isEqualTo("https://booking.example.com");
    }

    @Test
    void 등록_시_예약_방식과_예약_링크가_없으면_null로_저장한다() {
        Program program = program(null, null);

        assertThat(program.getReservationType()).isNull();
        assertThat(program.getReservationUrl()).isNull();
    }

    @Test
    void 수정_시_값이_있으면_예약_방식과_예약_링크를_변경한다() {
        Program program = program(ReservationType.ON_SITE, "https://old.example.com");

        update(program, ReservationType.FREE_ENTRY, "https://new.example.com");

        assertThat(program.getReservationType()).isEqualTo(ReservationType.FREE_ENTRY);
        assertThat(program.getReservationUrl()).isEqualTo("https://new.example.com");
    }

    @Test
    void 수정_시_null이면_예약_방식은_선택_안_함_예약_링크는_없음이_된다() {
        Program program = program(ReservationType.ON_SITE, "https://booking.example.com");

        update(program, null, null);

        assertThat(program.getReservationType()).isNull();
        assertThat(program.getReservationUrl()).isNull();
    }

    @Test
    void 수정_시_기존_값을_담아_보내면_예약_정보가_유지된다() {
        Program program = program(ReservationType.ON_SITE, "https://booking.example.com");

        update(program, ReservationType.ON_SITE, "https://booking.example.com");

        assertThat(program.getTitle()).isEqualTo("새 제목");
        assertThat(program.getReservationType()).isEqualTo(ReservationType.ON_SITE);
        assertThat(program.getReservationUrl()).isEqualTo("https://booking.example.com");
    }

    // 제목과 예약 정보만 넘기고 나머지는 null(유지)
    private void update(Program program, ReservationType reservationType, String reservationUrl) {
        program.update("새 제목", null, null, null, null, reservationType, reservationUrl,
                null, null, null, null, null, null, null, null, null, null);
    }

    private Program program(ReservationType reservationType, String reservationUrl) {
        return Program.builder()
                .title("전시")
                .programType(ProgramType.EXHIBITION)
                .description("소개")
                .reserved(false)
                .reservationType(reservationType)
                .reservationUrl(reservationUrl)
                .free(true)
                .venueName("장소")
                .address("서울")
                .build();
    }
}
