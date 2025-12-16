package io.github.jihyundev.spring_batch_etl.batch.exception;

/**
 * 회원 데이터 자체가 잘못된 경우 사용하는 예외
 * 재시도해도 해결되지 않으므로 retry 대상이 아니라 skip 대상으로만 사용
 */
public class InvalidDataException extends BatchBusinessException{
    public InvalidDataException(String message) {
        super("INVALID_DATA", message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super("INVALID_DATA", message, cause);
    }
}
