package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InvalidRequestException;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointServiceTest {
    @Autowired
    PointService pointService;

    @Test
    void 최소_충전_금액_만원_이내일_경우_예외_발생() {
        long userId = 1L;
        long amount = 5000L;

        assertThatThrownBy(() -> pointService.charge(userId, amount))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 천원_단위로_충전하지_않으면_예외_발생() {
        long userId = 1L;
        long amount = 5500L;

        assertThatThrownBy(() -> pointService.charge(userId, amount))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 포인트_충전() {
        // given
        long userId = 1L;
        long amount = 10000L;

        // when
        UserPoint point = pointService.charge(userId, amount);

        // then
        assertThat(point.point()).isEqualTo(amount);
    }

    @Test
    void 특정_유저의_포인트_조회() {
        // given
        long userId = 1L;
        long amount = 10000L;
        pointService.charge(userId, amount);

        // when
        UserPoint point = pointService.getPoint(userId);

        // then
        assertThat(point.point()).isEqualTo(amount);
    }


}