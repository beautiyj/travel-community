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

    /** 예약 거절(사업자, PAID 대상): 거절 사유 기록 */
    void reject(@Param("reservationId") Long reservationId, @Param("reason") String reason);

    /** 관리자 목록용: 특정 상태의 예약 조회 (예: CANCEL_REQUESTED 건 모아보기) */
    List<Reservation> findByStatus(@Param("status") ReservationStatus status);

    /** 스케줄러용: 예약 생성 후 24시간이 지나도록 미승인(PAID) 상태로 남은 예약 ID 목록 (방문일 무관) */
    List<Long> findExpiredUnapprovedPaidIds();

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
     * 예약 캘린더용: 장소의 '오늘 이후' 활성 예약 목록(체크인·체크아웃·인원).
     * 숙박은 기간 예약이라 날짜별 GROUP BY로는 중간 날짜를 알 수 없다.
     * 서비스에서 이 목록을 받아 구간을 날짜 단위로 펼쳐 마감일을 계산한다.
     */
    List<Map<String, Object>> findActiveRanges(@Param("placeId") Long placeId);

    /**
     * 숙박 기간 겹침 판정: 요청 구간과 겹치는 활성 예약 수 (0이면 예약 가능).
     * 반개구간이라 체크아웃 당일에 다음 손님이 체크인하는 것은 겹침이 아니다.
     * 기존 단일날짜 예약(check_out_date IS NULL)은 1박짜리로 취급한다.
     */
    int countOverlapping(@Param("placeId") Long placeId,
                         @Param("checkInDate") LocalDate checkInDate,
                         @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * 마감(휴무) 여부 조회. PLACE는 사업자(business) 파트 테이블 — 여기서는 읽기만 한다.
     * 사업자가 /business/closure에서 껐다 켰다 하는 그 값(is_closed)을 그대로 참조.
     */
    Integer findPlaceClosed(@Param("placeId") Long placeId);

    /** 장소 타입(tour/food/stay) 조회. PLACE.place_type을 읽기 전용으로 참조 */
    String findPlaceType(@Param("placeId") Long placeId);

    /** 장소 이름 조회. PLACE.name을 읽기 전용으로 참조 (예약 폼 표시용) */
    String findPlaceName(@Param("placeId") Long placeId);

    /** 숙박 1인 단가 조회. PLACE.min_price를 읽기 전용으로 참조 (price_type은 안 보고 값만 그대로 씀) */
    Integer findMinPrice(@Param("placeId") Long placeId);

    /**
     * 날짜별 마감(PLACE_CLOSED_DATE) 겹침 체크: 요청 기간([fromDate, toDate))과 겹치는 마감일 수.
     * 0이면 마감일과 안 겹침. PLACE_CLOSED_DATE도 business 파트 테이블 — 읽기만 한다.
     */
    int countClosedDatesInRange(@Param("placeId") Long placeId,
                                @Param("fromDate") LocalDate fromDate,
                                @Param("toDate") LocalDate toDate);

    /** 예약 캘린더용: 오늘 이후 날짜별 마감일(PLACE_CLOSED_DATE) 목록 */
    List<LocalDate> findClosedDates(@Param("placeId") Long placeId);

    /** 맛집·관광지 정원 체크용: 특정 날짜의 활성(PENDING/PAID/CONFIRMED) 예약 인원 합계 */
    int sumHeadcountOnDate(@Param("placeId") Long placeId, @Param("visitDate") LocalDate visitDate);
}
