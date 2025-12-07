package io.github.jihyundev.spring_batch_etl.batch.exception;

public class TransientApiException extends BatchBusinessException{

    public TransientApiException(String message) {
        super("TRANSIENT_API_ERROR", message);
    }

    public TransientApiException(String message, Throwable cause) {
        super("TRANSIENT_API_ERROR", message, cause);
    }
}
