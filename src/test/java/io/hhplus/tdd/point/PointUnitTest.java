package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.ErrorCode;
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

import java.util.ArrayList;
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

        UserPoint UserPointFromDb = new UserPoint(userId, initialBalance, System.currentTimeMillis());

        long totalAmount = UserPointFromDb.point() + chargeAmount;


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
        long initialBalance = 9990000L;
        long chargeAmount = 11000L;

        UserPoint UserPointFromDb = new UserPoint(userId, initialBalance, System.currentTimeMillis());

        when(mockUserPointTable.selectById(userId)).thenReturn(UserPointFromDb);

        // then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.EXCEED_AMOUNT.getMessage());

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }

    @Test
    void 충전_금액_천원_단위_아닐때_예외_발생() {
        //given
        long chargeAmount = 99990L;

        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.NOT_UNIT_OF_TEN_THOUSAND.getMessage());

        verify(mockUserPointTable, never()).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 충전_금액_최소_만원_미만일때_예외_발생() {
        //given
        long chargeAmount = 9999L;

        //then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.CHARGE_AMOUNT_TOO_LOW.getMessage());

        verify(mockUserPointTable, never()).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
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
                .hasMessageContaining(ErrorCode.INSUFFICIENT_POINT.getMessage());

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 포인트_사용_성공() {
        // given
        long initialAmount = 90000L;
        long amountToUse = 5000L;

        long totalAmount = initialAmount - amountToUse;

        UserPoint currentUserPoint = new UserPoint(userId, initialAmount, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, totalAmount, System.currentTimeMillis());


        when(mockUserPointTable.selectById(userId)).thenReturn(currentUserPoint);
        when(mockUserPointTable.insertOrUpdate(userId, totalAmount)).thenReturn(expectedUserPoint);

        // when
        UserPoint userPoint = pointService.use(userId, amountToUse);

        // then
        assertThat(userPoint.point()).isEqualTo(totalAmount);

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockPointHistoryTable, times(1)).insert(eq(userId), eq(amountToUse), eq(TransactionType.USE), anyLong());
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
                .hasMessageContaining(ErrorCode.INSUFFICIENT_POINT.getMessage());

        verify(mockUserPointTable, times(1)).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 포인트_사용_최소_금액_미만일때_예외_발생() {
        //given
        long pointToUse = 4999L;

        //then
        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.USE_AMOUNT_TOO_LOW.getMessage());

        verify(mockUserPointTable, never()).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 포인트_사용_시_음수_금액_입력시_예외_발생() {
        //given
        long pointToUse = -2000L;

        //then
        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.NEGATIVE_USE_AMOUNT_NOT_ALLOWED.getMessage());

        verify(mockUserPointTable, never()).selectById(userId);
        verify(mockUserPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(mockPointHistoryTable, never()).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }

    @Test
    void 포인트_사용_시_0원_입력시_예외_발생() {
        //given
        long pointToUse = 0;

        //then
        assertThatThrownBy(() -> pointService.use(userId, pointToUse))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(ErrorCode.ZERO_USE_AMOUNT_NOT_ALLOWED.getMessage());

        verify(mockUserPointTable, never()).selectById(userId);
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

    @Test
    void 포인트_내역_조회시_여러_기록_정상_반환() {
        //given
        long amount = 100000L;
        long useAmount = 65000L;

        PointHistory userPoint1 = new PointHistory(1L, userId, amount,TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory userPoint2 = new PointHistory(2L, userId, amount,TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory userPoint3 = new PointHistory(3L, userId, useAmount,TransactionType.USE, System.currentTimeMillis());

        List<PointHistory> pointHistories = new ArrayList<>();

        pointHistories.add(userPoint1);
        pointHistories.add(userPoint2);
        pointHistories.add(userPoint3);

        when(mockPointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

        // when
        List<PointHistory> actualHistories = pointService.getHistory(userId);

        //then
        assertThat(actualHistories).isNotNull();
        assertThat(actualHistories.size()).isEqualTo(3);
        assertThat(actualHistories).isEqualTo(pointHistories);
        assertThat(actualHistories.get(0).amount()).isEqualTo(amount);
        assertThat(actualHistories.get(2).type()).isEqualTo(TransactionType.USE);

        verify(mockPointHistoryTable, times(1)).selectAllByUserId(userId);

    }

}
