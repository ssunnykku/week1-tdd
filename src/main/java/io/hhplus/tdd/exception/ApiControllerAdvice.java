package io.hhplus.tdd.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }


    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[BaseException] {}", errorCode.getMessage(), e);
        log.error("[BaseException] 예외 발생: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(String.valueOf(errorCode.getStatus().value()), errorCode.getMessage()));
    }


}
