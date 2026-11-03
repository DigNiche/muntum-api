package com.digniche.muntum.global.exception;

import java.sql.SQLException;
import java.util.Optional;

/**
 * 같은 요청을 다시 보내면 성공할 수 있는 MySQL 락 충돌 판별
 * - 1213: 데드락 (MySQL이 한쪽 트랜잭션을 롤백)
 * - 1205: 락 대기 시간 초과
 * - FK 위반(1451, 1452) 등 다시 보내도 해결된다는 보장이 없는 오류는 포함하지 않는다
 */
public final class RetryableLockFailure {

    public static final int DEADLOCK = 1213;
    public static final int LOCK_WAIT_TIMEOUT = 1205;

    private RetryableLockFailure() {
    }

    /**
     * 예외의 원인을 따라가며 재시도 가능한 MySQL 오류 번호를 찾는다
     */
    public static Optional<Integer> findErrorCode(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlException) {
                int errorCode = sqlException.getErrorCode();
                if (errorCode == DEADLOCK || errorCode == LOCK_WAIT_TIMEOUT) {
                    return Optional.of(errorCode);
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return Optional.empty();
    }
}
