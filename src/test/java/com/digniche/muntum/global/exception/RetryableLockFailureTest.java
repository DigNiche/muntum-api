package com.digniche.muntum.global.exception;

import com.digniche.muntum.global.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재시도 가능한 락 충돌 판별과 응답
 * - 데드락(1213)·락 대기 시간 초과(1205)만 "잠시 후 다시 시도"(G004, 503)
 * - FK 위반(1451, 1452) 등은 다시 보내도 해결된다는 보장이 없으므로 서버 오류(E001, 500)
 */
class RetryableLockFailureTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/curations/1/like");

    @Test
    void 원인_안의_데드락과_락_대기_시간_초과는_재시도_대상이다() {
        assertThat(RetryableLockFailure.findErrorCode(wrapped(1213))).contains(1213);
        assertThat(RetryableLockFailure.findErrorCode(wrapped(1205))).contains(1205);
    }

    @Test
    void FK_위반과_SQL_예외가_없는_오류는_재시도_대상이_아니다() {
        assertThat(RetryableLockFailure.findErrorCode(wrapped(1451))).isEmpty();
        assertThat(RetryableLockFailure.findErrorCode(wrapped(1452))).isEmpty();
        assertThat(RetryableLockFailure.findErrorCode(new IllegalStateException("no sql"))).isEmpty();
    }

    @Test
    void 데드락은_잠시_후_다시_시도_에러로_응답한다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataAccessException(
                new CannotAcquireLockException("deadlock", sqlException(1213)), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.RETRY_LATER.getCode());
    }

    @Test
    void FK_위반은_서버_오류로_응답한다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataAccessException(
                new DataIntegrityViolationException("fk", sqlException(1451)), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.SERVER_ERROR.getCode());
    }

    // 실제 예외처럼 Spring·Hibernate 예외 안쪽에 SQLException이 있는 모양
    private static RuntimeException wrapped(int mysqlErrorCode) {
        return new RuntimeException("spring", new RuntimeException("hibernate", sqlException(mysqlErrorCode)));
    }

    private static SQLException sqlException(int mysqlErrorCode) {
        return new SQLException("mysql error " + mysqlErrorCode, "HY000", mysqlErrorCode);
    }
}
