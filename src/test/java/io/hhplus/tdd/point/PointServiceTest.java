package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InvalidRequestException;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointServiceTest {
    @Autowired
    PointService pointService;

    private long userId;

    @BeforeEach
    void setUp() {
        userId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    @Test
    void 최소_충전_금액_만원_이내일_경우_예외_발생() {
        long amount = 5000L;

        assertThatThrownBy(() -> pointService.charge(userId, amount))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 천원_단위로_충전하지_않으면_예외_발생() {
        long amount = 5500L;

        assertThatThrownBy(() -> pointService.charge(userId, amount))
                .isInstanceOf(InvalidRequestException.class);
    }


    @Test
    void 포인트_충전_기록이_정상적으로_저장된다() {
        long amount = 10000L;
        pointService.charge(userId, amount);

        List<PointHistory> histories = pointService.getHistory(userId);

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).amount()).isEqualTo(amount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    void 특정_유저의_포인트_사용() {
        long amount = 200000L;
        long pointToUse = 50000L;
        pointService.charge(userId, amount);

        List<PointHistory> histories = pointService.getHistory(userId);

        UserPoint point = pointService.use(userId, pointToUse);

        assertThat(point.point()).isEqualTo(amount - pointToUse);
    }

    @Test
    void 포인트_잔고_부족시_예외_발생() {
        long pointToUse = 30000L;

        List<PointHistory> histories = pointService.getHistory(userId);

        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 최소_사용_가능_금액_오천원_이내일_경우_예외_발생() {
        long amount = 4000L;

        assertThatThrownBy(() -> pointService.use(userId, amount))
                .isInstanceOf(InvalidRequestException.class);
    }


    @Test
    void 포인트_충전() {
        // given
        long amount = 10000L;

        // when
        UserPoint point = pointService.charge(userId, amount);

        // then
        assertThat(point.point()).isEqualTo(amount);
    }

    @Test
    void 특정_유저의_포인트_조회() {
        // given
        long amount = 10000L;
        pointService.charge(userId, amount);

        // when
        UserPoint point = pointService.getPoint(userId);

        // then
        assertThat(point.point()).isEqualTo(amount);
    }


}