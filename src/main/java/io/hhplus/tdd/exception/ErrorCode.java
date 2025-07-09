package io.hhplus.tdd.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    CHARGE_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "충전 금액은 1만 원 이상이어야 합니다."),
    USE_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "사용 금액은 5만 원 이상이어야 합니다."),
    NOT_UNIT_OF_TEN_THOUSAND(HttpStatus.BAD_REQUEST, "충전은 만원 단위로 가능합니다.");

    private final HttpStatus status;
    private final String message;
}