package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.ErrorCode;
import io.hhplus.tdd.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPointTest {
    @Test
    void 포인트_충전() {
        UserPoint point = new UserPoint(1L, 10000L, System.currentTimeMillis());
        UserPoint updated = point.addPoint(5000L);

        assertThat(updated.point()).isEqualTo(15000L);
    }

    @Test
    void 포인트_사용시_잔액보다_많으면_예외() {
        UserPoint point = new UserPoint(1L, 5000L, System.currentTimeMillis());

        assertThatThrownBy(() -> point.usePoint(6000L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

    @Test
    void 포인트_사용() {
        UserPoint point = new UserPoint(1L, 10000L, System.currentTimeMillis());
        UserPoint updated = point.usePoint(4000L);

        assertThat(updated.point()).isEqualTo(6000L);
    }

}