package com.gnagnoohc.travel.reservation.mapper;

import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {
    void insert(Reservation reservation);
    Reservation findById(Long reservationId);
    List<Reservation> findByMemberId(Long memberId);
    void updateStatus(@Param("reservationId") Long reservationId, @Param("status") ReservationStatus status);

    /** 취소 요청: 상태를 CANCEL_REQUESTED로 바꾸고 사유·요청시각 기록 */
    void requestCancel(@Param("reservationId") Long reservationId, @Param("reason") String reason);

    /** 취소 요청 거절: 상태를 PAID로 원복하고 취소 요청 기록(사유·시각)도 함께 지움 */
    void rejectCancel(@Param("reservationId") Long reservationId);

    /** 관리자 목록용: 특정 상태의 예약 조회 (예: CANCEL_REQUESTED 건 모아보기) */
    List<Reservation> findByStatus(@Param("status") ReservationStatus status);

    /**
     * 슬롯 선점 체크: 같은 회원이 같은 장소·날짜에 가진 활성(PENDING/PAID) 예약 1건. 없으면 null.
     * PENDING이면 재사용(결제 이어가기), PAID면 중복 거부 판단에 쓴다.
     */
    Reservation findActiveBySlot(@Param("memberId") Long memberId,
                                 @Param("placeId") Long placeId,
                                 @Param("visitDate") LocalDate visitDate);

    /** 스케줄러: cutoff 이전에 생성됐는데 아직 PENDING인 건을 EXPIRED로 일괄 전환. 처리 건수 반환 */
    int expirePending(@Param("cutoff") LocalDateTime cutoff);

    /** 스케줄러: 방문일이 지난 PAID 예약을 COMPLETED로 일괄 전환. 처리 건수 반환 */
    int completeVisited();
}
