package io.github.jihyundev.spring_batch_etl.batch.exception;

import lombok.Getter;

@Getter
public class BatchBusinessException extends RuntimeException {
    private final String errorCode;

    protected BatchBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BatchBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
