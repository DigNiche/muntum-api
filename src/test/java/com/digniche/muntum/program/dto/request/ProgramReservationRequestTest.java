package com.digniche.muntum.program.dto.request;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.exception.GlobalExceptionHandler;
import com.digniche.muntum.program.entity.ReservationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 프로그램 등록·수정 요청의 예약 방식·예약 링크 역직렬화와 검증
 * - 잘못된 예약 방식 값은 GlobalExceptionHandler에서 P005로 응답
 */
class ProgramReservationRequestTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 예약_방식_4종을_역직렬화한다() throws Exception {
        for (ReservationType type : ReservationType.values()) {
            ProgramUpdateRequest request = read("{\"reservationType\":\"" + type.name() + "\"}");

            assertThat(request.reservationType()).isEqualTo(type);
        }
    }

    @Test
    void 예약_방식과_예약_링크가_null이면_null로_받는다() throws Exception {
        ProgramUpdateRequest request = read("{\"reservationType\":null,\"reservationUrl\":null}");

        assertThat(request.reservationType()).isNull();
        assertThat(request.reservationUrl()).isNull();
    }

    @Test
    void 정의되지_않은_예약_방식은_P005로_응답한다() {
        InvalidFormatException cause = catchThrowableOfType(
                InvalidFormatException.class,
                () -> read("{\"reservationType\":\"ONSITE\"}"));
        assertThat(cause.getTargetType()).isEqualTo(ReservationType.class);

        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler().handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error", cause, new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.INVALID_RESERVATION_TYPE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INVALID_RESERVATION_TYPE.getMessage());
    }

    @Test
    void 예약_방식에_빈_문자열을_보내면_역직렬화에_실패한다() {
        assertThatThrownBy(() -> read("{\"reservationType\":\"\"}"))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void 예약_링크가_500자를_넘으면_검증에_실패한다() throws Exception {
        ProgramUpdateRequest valid = read("{\"reservationUrl\":\"" + "a".repeat(500) + "\"}");
        ProgramUpdateRequest tooLong = read("{\"reservationUrl\":\"" + "a".repeat(501) + "\"}");

        assertThat(reservationUrlViolations(valid)).isZero();
        assertThat(reservationUrlViolations(tooLong)).isEqualTo(1);
    }

    @Test
    void 등록_요청의_예약_정보가_엔티티에_반영된다() throws Exception {
        ProgramCreateRequest request = objectMapper.readValue(
                "{\"title\":\"전시\",\"programType\":\"EXHIBITION\",\"description\":\"소개\","
                        + "\"free\":true,\"venueName\":\"장소\",\"address\":\"서울\","
                        + "\"reservationType\":\"ON_SITE\",\"reservationUrl\":\"https://booking.example.com\"}",
                ProgramCreateRequest.class);

        var program = request.toEntity();

        assertThat(program.getReservationType()).isEqualTo(ReservationType.ON_SITE);
        assertThat(program.getReservationUrl()).isEqualTo("https://booking.example.com");
    }

    // 삭제된 reserved 필드를 이전 버전 앱이 계속 보내도 모르는 필드로 무시하고 정상 처리
    @Test
    void 이전_버전_앱이_reserved를_보내도_무시하고_등록_수정_요청을_받는다() throws Exception {
        String json = "{\"title\":\"전시\",\"programType\":\"EXHIBITION\",\"description\":\"소개\",\"reserved\":true,"
                + "\"free\":true,\"venueName\":\"장소\",\"address\":\"서울\",\"reservationType\":\"PRE_REGISTRATION\"}";

        ProgramCreateRequest create = objectMapper.readValue(json, ProgramCreateRequest.class);
        ProgramUpdateRequest update = objectMapper.readValue(json, ProgramUpdateRequest.class);

        assertThat(validator.validate(create)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();
        assertThat(create.toEntity().getReservationType()).isEqualTo(ReservationType.PRE_REGISTRATION);
        assertThat(update.reservationType()).isEqualTo(ReservationType.PRE_REGISTRATION);
    }

    private ProgramUpdateRequest read(String json) throws Exception {
        return objectMapper.readValue(json, ProgramUpdateRequest.class);
    }

    private long reservationUrlViolations(ProgramUpdateRequest request) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals("reservationUrl"))
                .count();
    }
}
