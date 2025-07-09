package io.hhplus.tdd.exception;

public class InvalidRequestException extends BaseException {
    public InvalidRequestException(ErrorCode errorCode) {

        super(errorCode);
    }
}