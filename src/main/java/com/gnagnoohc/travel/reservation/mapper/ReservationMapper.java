package com.gnagnoohc.travel.reservation.mapper;

import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReservationMapper {
    void insert(Reservation reservation);
    Reservation findById(Long reservationId);
    List<Reservation> findByMemberId(Integer memberId);
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
    Reservation findActiveBySlot(@Param("memberId") Integer memberId,
                                 @Param("placeId") Long placeId,
                                 @Param("visitDate") LocalDate visitDate);

    /** 결제 준비 시: 발급한 orderId·시도 PG·(카카오)tid를 예약 행에 기록 (정합성 보정 배치의 조회 키 확보) */
    void markPaymentReady(@Param("reservationId") Long reservationId,
                          @Param("orderId") String orderId,
                          @Param("paymentType") int paymentType,
                          @Param("pgTid") String pgTid);

    /** 정합성 보정 배치: 결제 준비까지 갔으나(order_id 있음) cutoff 이전 생성돼 아직 PENDING인 건 조회 */
    List<Reservation> findReadyForInquiry(@Param("cutoff") LocalDateTime cutoff);

    /** 스케줄러: cutoff 이전에 생성됐는데 아직 PENDING인 건을 EXPIRED로 일괄 전환. 처리 건수 반환 */
    int expirePending(@Param("cutoff") LocalDateTime cutoff);

    /** 스케줄러: 방문일이 지난 PAID 예약을 COMPLETED로 일괄 전환. 처리 건수 반환 */
    int completeVisited();

    /**
     * 예약 캘린더용: 장소의 '오늘 이후' 활성 예약이 있는 날짜 목록(+인원 합).
     * 이 목록에 있는 날 = 이미 예약된 날 → 마감(빨강)으로 판정한다. (1일 1팀)
     */
    List<Map<String, Object>> findBookedHeadcountByDate(@Param("placeId") Long placeId);

    /**
     * 마감 백업 체크: 특정 슬롯(장소·날짜)의 활성 예약 인원 합 (전체 회원 대상, 없으면 0).
     * 0보다 크면 이미 예약된 날 → 예약 생성 직전 서버에서 재예약을 막는 최종 방어선.
     */
    int sumActiveHeadcount(@Param("placeId") Long placeId, @Param("visitDate") LocalDate visitDate);

    /**
     * 마감(휴무) 여부 조회. PLACE는 사업자(business) 파트 테이블 — 여기서는 읽기만 한다.
     * 사업자가 /business/closure에서 껐다 켰다 하는 그 값(is_closed)을 그대로 참조.
     */
    Boolean findPlaceClosed(@Param("placeId") Long placeId);
}
