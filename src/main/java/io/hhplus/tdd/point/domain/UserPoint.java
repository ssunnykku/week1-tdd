package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.exception.ErrorCode;
import io.hhplus.tdd.exception.InvalidRequestException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint addPoint(long amount) {
        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }

    public UserPoint usePoint(long amount) {
        if (amount > this.point) {
            throw new InvalidRequestException(ErrorCode.INSUFFICIENT_POINT);
        }
        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }


}
