package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.dto.ReservationCreateRequest;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 임시 1인 단가. 숙박/맛집 파트의 가격 컬럼이 확정되면 그쪽 조회로 교체 */
    public static final int TEMP_UNIT_PRICE = 10000;

    /** 만나서 결제(관광지·맛집)의 예약금 정액. TODO: place별 예약금 데이터가 생기면 조회로 교체 */
    public static final int RESERVE_DEPOSIT = 10000;

    /**
     * 만나서 결제(예약금만 선결제) 대상인지. tour(관광지)·food(맛집)만 예약금, 그 외(숙박 등)는 정가.
     * 프론트(reservation-form.js)·뷰의 분기와 동일 규칙.
     */
    public static boolean isPayOnSite(String placeType) {
        return "tour".equals(placeType) || "food".equals(placeType);
    }

    private final ReservationMapper reservationMapper;

    /** 예약 생성 (결제 전이므로 PENDING 상태). 같은 장소·날짜 중복 예약은 거부 */
    @Transactional
    public Long create(Integer memberId, ReservationCreateRequest req) {
        // 같은 슬롯(회원·장소·날짜)에 활성 예약이 있으면:
        //   - PENDING(미결제): 결제하러 갔다가 돌아온 경우이므로 그 예약을 재사용해 결제를 이어가게 한다
        //   - PAID(결제완료): 이미 확정된 예약이므로 새 예약을 거부한다
        Reservation existing = reservationMapper.findActiveBySlot(memberId, req.getPlaceId(), req.getVisitDate());
        if (existing != null) {
            if (existing.getStatus() == ReservationStatus.PENDING) {
                return existing.getReservationId();
            }
            throw new IllegalStateException("이미 예약이 완료된 날짜입니다.");
        }

        // 마감 백업 체크(2겹): 숙박만 '1일 1팀'이라 그 날 활성 예약이 하나라도 있으면 거부.
        // 맛집/관광지(만나서결제)는 하루 여러 팀 가능 → 이 체크를 건너뛴다. (프론트 캘린더와 동일 규칙)
        if (!isPayOnSite(req.getPlaceType())) {
            int booked = reservationMapper.sumActiveHeadcount(req.getPlaceId(), req.getVisitDate());
            if (booked > 0) {
                throw new IllegalStateException("이미 예약된 날짜입니다. 다른 날짜를 선택해 주세요.");
            }
        }

        Reservation r = new Reservation();
        r.setMemberId(memberId);
        r.setPlaceId(req.getPlaceId());
        r.setVisitorName(req.getVisitorName());
        r.setPhone(req.getPhone());
        r.setVisitDate(req.getVisitDate());
        r.setHeadcount(req.getHeadcount());
        r.setPlaceType(req.getPlaceType());   // tour/food면 이후 결제금액이 예약금으로 계산됨
        r.setStatus(ReservationStatus.PENDING);
        reservationMapper.insert(r);
        return r.getReservationId();
    }

    @Transactional(readOnly = true)
    public Reservation getById(Long reservationId) {
        Reservation r = reservationMapper.findById(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("존재하지 않는 예약입니다. id=" + reservationId);
        }
        return r;
    }

    @Transactional(readOnly = true)
    public List<Reservation> getMyReservations(Integer memberId) {
        return reservationMapper.findByMemberId(memberId);
    }

    /**
     * 예약 캘린더용 마감 현황.
     * 반환: { "booked": { "yyyy-MM-dd": 예약인원합, ... } } — 활성예약이 있는 날만 담긴다.
     * 프론트는 booked 에 들어있는 날(=이미 예약된 날)을 마감(빨강·선택불가)으로 그린다. (1일 1팀)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(Long placeId) {
        Map<String, Integer> booked = new LinkedHashMap<>();
        for (Map<String, Object> row : reservationMapper.findBookedHeadcountByDate(placeId)) {
            // visitDate 는 java.sql.Date, booked 는 SUM 결과(Long) — 문자열 날짜 → 인원수로 담는다
            String date = String.valueOf(row.get("visitDate"));
            int count = ((Number) row.get("booked")).intValue();
            booked.put(date, count);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("booked", booked);
        return result;
    }

    @Transactional
    public void updateStatus(Long reservationId, ReservationStatus status) {
        reservationMapper.updateStatus(reservationId, status);
    }

    /**
     * 결제 준비 시 발급한 orderId와 시도 PG를 예약 행에 기록.
     * 이후 정합성 보정 배치가 이 orderId로 PG에 "실제 결제됐는지" 조회할 수 있게 된다.
     * (지금은 orderId가 세션에만 있어 콜백을 놓치면 유령 결제를 추적할 수 없다)
     */
    @Transactional
    public void markPaymentReady(Long reservationId, String orderId, int paymentType, String pgTid) {
        reservationMapper.markPaymentReady(reservationId, orderId, paymentType, pgTid);
    }

    /**
     * 관리자: 예약 승인 (결제완료 → 예약확정).
     * 결제만 끝난(PAID) 예약을 관리자가 검토해 확정(CONFIRMED)으로 올린다.
     * 승인 버튼·화면은 관리자(사업자) 파트에 있고, 그쪽에서 이 메서드를 호출한다.
     */
    @Transactional
    public void approve(Long reservationId) {
        Reservation r = getById(reservationId);
        if (r.getStatus() != ReservationStatus.PAID) {
            throw new IllegalStateException("결제완료된 예약만 승인할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }
        reservationMapper.updateStatus(reservationId, ReservationStatus.CONFIRMED);
    }

    /**
     * 취소 요청. 상태를 CANCEL_REQUESTED로 바꾸고 사유 기록. 환불은 관리자 승인 시 실행.
     * 본인 예약 + 결제완료(PAID)/예약확정(CONFIRMED) 상태에서만 가능.
     */
    @Transactional
    public void requestCancel(Long reservationId, Integer memberId, String reason) {
        Reservation r = getById(reservationId);
        if (!r.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 예약만 취소 요청할 수 있습니다.");
        }
        if (r.getStatus() != ReservationStatus.PAID && r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("결제완료 또는 예약확정 상태에서만 취소 요청할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }
        reservationMapper.requestCancel(reservationId, reason);
    }

    /**
     * 관리자: 취소 요청 거절. CANCEL_REQUESTED → PAID로 원복 (환불 없음).
     * 거절된 예약에 취소 사유·요청시각이 남아 있으면 혼란스러우므로 함께 지운다.
     */
    @Transactional
    public void rejectCancel(Long reservationId) {
        Reservation r = getById(reservationId);
        if (r.getStatus() != ReservationStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 요청 상태인 예약만 거절할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }
        reservationMapper.rejectCancel(reservationId);
    }

    /** 관리자 목록용: 특정 상태의 예약 조회 */
    @Transactional(readOnly = true)
    public List<Reservation> getByStatus(ReservationStatus status) {
        return reservationMapper.findByStatus(status);
    }

    /**
     * 결제 금액 계산 (모든 결제수단이 이 값을 공통으로 사용).
     * - 관광지·맛집(만나서 결제): 예약금 정액 RESERVE_DEPOSIT (인원 무관)
     * - 그 외(숙박): 인원 × 단가
     * TODO: 숙박/맛집 팀원의 place(가격) 테이블이 나오면 placeId로 단가·예약금 조회하도록 교체
     */
    public int calculateAmount(Reservation r) {
        if (isPayOnSite(r.getPlaceType())) {
            return RESERVE_DEPOSIT;
        }
        return r.getHeadcount() * TEMP_UNIT_PRICE;
    }
}
