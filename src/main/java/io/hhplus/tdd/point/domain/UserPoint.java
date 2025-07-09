package io.hhplus.tdd.point.domain;

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

}
