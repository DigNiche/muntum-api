package com.digniche.muntum.program.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로그램 예약 방식·예약 링크 저장 및 수정
 * - 수정 정책은 다른 필드와 동일 : null이면 기존 값 유지, 값이 있으면 변경
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
    void 수정_시_null이면_기존_예약_방식과_예약_링크를_유지한다() {
        Program program = program(ReservationType.ON_SITE, "https://booking.example.com");

        update(program, null, null);

        assertThat(program.getReservationType()).isEqualTo(ReservationType.ON_SITE);
        assertThat(program.getReservationUrl()).isEqualTo("https://booking.example.com");
    }

    @Test
    void 수정_시_값이_있으면_예약_방식과_예약_링크를_변경한다() {
        Program program = program(ReservationType.ON_SITE, "https://old.example.com");

        update(program, ReservationType.FREE_ENTRY, "https://new.example.com");

        assertThat(program.getReservationType()).isEqualTo(ReservationType.FREE_ENTRY);
        assertThat(program.getReservationUrl()).isEqualTo("https://new.example.com");
    }

    @Test
    void 수정_시_예약_방식만_보내면_예약_링크는_유지한다() {
        Program program = program(ReservationType.ON_SITE, "https://booking.example.com");

        update(program, ReservationType.PRE_REGISTRATION_AND_ON_SITE, null);

        assertThat(program.getReservationType()).isEqualTo(ReservationType.PRE_REGISTRATION_AND_ON_SITE);
        assertThat(program.getReservationUrl()).isEqualTo("https://booking.example.com");
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

    // 예약 정보만 수정하고 나머지는 null(유지)
    private void update(Program program, ReservationType reservationType, String reservationUrl) {
        program.update(null, null, null, null, null,
                reservationType, reservationUrl,
                null, null, null, null, null, null, null, null, null, null);
    }
}
