package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.ErrorCode;
import io.hhplus.tdd.exception.InvalidRequestException;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;

    private final PointHistoryTable pointHistoryTable;

    // 포인트 조회
    public UserPoint getPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    // 포인트 충전
    public UserPoint charge(long userId, long amount) {

        if (amount < 10000) {
            throw new InvalidRequestException(ErrorCode.CHARGE_AMOUNT_TOO_LOW);
        }

        if (amount % 1000 != 0) {
            throw new InvalidRequestException(ErrorCode.NOT_UNIT_OF_TEN_THOUSAND);
        }

        UserPoint currentPoint = userPointTable.selectById(userId);
        UserPoint chargedPoint = currentPoint.addPoint(amount);

        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, chargedPoint.point());
    }

}
