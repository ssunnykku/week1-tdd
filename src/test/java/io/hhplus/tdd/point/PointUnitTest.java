package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InvalidRequestException;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointUnitTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable mockUserPointTable;

    @Mock
    private PointHistoryTable mockPointHistoryTable;

    private long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    // 포인트 충전
    @Test
    void 포인트_충전() {
        // given
        long amount = 10000L;

        UserPoint initialUserPoint = UserPoint.empty(userId);
        UserPoint expectedChargedPoint = new UserPoint(userId, amount, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(initialUserPoint);
        when(mockUserPointTable.insertOrUpdate(userId, amount)).thenReturn(expectedChargedPoint);

        // when
        UserPoint point = pointService.charge(userId, amount);

        // then
        assertThat(point.point()).isEqualTo(amount);
        assertThat(point.id()).isEqualTo(userId);

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, times(1)).insertOrUpdate(userId, amount);
        verify(mockPointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void 잔액_있는_경우_포인트_충전() {
        // given
        long initialBalance = 5000L;
        long chargeAmount = 20000L;

        long totalAmount = initialBalance + chargeAmount;

        UserPoint UserPointFromDb = new UserPoint(userId, initialBalance, System.currentTimeMillis());
        UserPoint expectedChargedPoint = new UserPoint(userId, totalAmount, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(UserPointFromDb);
        when(mockUserPointTable.insertOrUpdate(eq(userId), eq(totalAmount))).thenReturn(expectedChargedPoint);

        // when
        UserPoint point = pointService.charge(userId, chargeAmount);

        // then
        assertThat(point.point()).isEqualTo(totalAmount);
        assertThat(point.id()).isEqualTo(userId);

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, times(1)).insertOrUpdate(eq(userId), eq(totalAmount));
        verify(mockPointHistoryTable, times(1)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void 천만원_초과_충전시_예외_발생() {
        // given
        long initialBalance = 10000000L;
        long chargeAmount = 10000L;
        long totalAmount = initialBalance + chargeAmount;

        UserPoint UserPointFromDb = new UserPoint(userId, initialBalance, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(UserPointFromDb);

        // then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("충전 가능 금액 1000만원을 초과했습니다.");

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }

    // 포인트 사용
    @Test
    void 포인트_사용_잔고_부족시_예외_발생() {
        // given
        long initialAmount = 5000L;
        long pointToUse = 10000L;

        UserPoint currentUserPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(currentUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("포인트가 부족합니다.");

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 포인트_사용_성공() {
        // given
        long initialAmount = 10000L;
        long amount = 5000L;

        long totalAmount = initialAmount - amount;

        UserPoint currentUserPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());
        UserPoint usePoint = new UserPoint(userId, totalAmount, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(currentUserPoint);
        when(mockUserPointTable.insertOrUpdate(userId, amount)).thenReturn(usePoint);
        // when

        // then
        assertThat(pointService.use(userId, amount).point()).isEqualTo(initialAmount - amount);

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockPointHistoryTable, times(1)).insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
    }


    @Test
    void 포인트_잔고_0일경우_포인트_사용시_예외_발생() {
        // given
        long initialAmount = 0L;
        long pointToUse = 10000L;

        UserPoint currentUserPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(currentUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("포인트가 부족합니다.");

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    // 포인트 조회
    @Test
    void 존재하는_사용자_포인트_조회() {
        // given
        long initialAmount = 10000L;
        UserPoint currentUserPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());
        when(mockUserPointTable.selectById(userId)).thenReturn(currentUserPoint);

        // then
        assertThat(pointService.getPoint(userId).point()).isEqualTo(initialAmount);
        verify(mockUserPointTable, times(1)).selectById(userId);
    }

    @Test
    void 특정_유저의_포인트_내역_조회시_기록_없을_경우() {
        // given
        when(mockPointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        List<PointHistory> histories = pointService.getHistory(userId);

        // then
        assertThat(histories).isNotNull();
        assertThat(histories).isEmpty(); // 또는 .hasSize(0);

        // verify
        verify(mockPointHistoryTable, times(1)).selectAllByUserId(userId);
    }


}
