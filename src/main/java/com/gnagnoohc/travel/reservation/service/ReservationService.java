package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.dto.ReservationCreateRequest;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 만나서 결제(관광지·맛집)의 예약금 정액. TODO: place별 예약금 데이터가 생기면 조회로 교체 */
    public static final int RESERVE_DEPOSIT = 10000;

    /** 맛집·관광지 하루 정원(명). 캘린더에 "남은 자리" 표시 + create()에서 초과 시 예약 차단에 쓴다. */
    public static final int DAILY_CAPACITY = 15;

    /**
     * 만나서 결제(예약금만 선결제) 대상인지. tour(관광지)·food(맛집)만 예약금, 그 외(숙박 등)는 정가.
     * 프론트(reservation-form.js)·뷰의 분기와 동일 규칙.
     */
    public static boolean isPayOnSite(String placeType) {
        return "tour".equals(placeType) || "food".equals(placeType);
    }

    /** 기간 예약(체크인·체크아웃) 대상인지. 숙박만 해당 — 맛집/관광지·무료는 당일 방문 */
    public static boolean isStay(String placeType) {
        return !isPayOnSite(placeType) && !"free".equals(placeType);
    }

    /** 숙박 박수. 체크아웃이 없으면(맛집/관광지 등) 1로 취급 */
    public static int nightsOf(Reservation r) {
        if (r.getCheckOutDate() == null || r.getVisitDate() == null) {
            return 1;
        }
        long nights = ChronoUnit.DAYS.between(r.getVisitDate(), r.getCheckOutDate());
        return nights > 0 ? (int) nights : 1;
    }

    private final ReservationMapper reservationMapper;

    /** 예약 생성 (결제 전이므로 PENDING 상태). 같은 장소·날짜 중복 예약은 거부 */
    @Transactional
    public Long create(Integer memberId, ReservationCreateRequest req) {
        // 사업자가 휴무로 설정한 장소는 예약 자체를 막는다 (PLACE.is_closed 읽기 전용 참조)
        if (Integer.valueOf(1).equals(reservationMapper.findPlaceClosed(req.getPlaceId()))) {
            throw new IllegalStateException("휴무 중인 장소입니다.");
        }

        // 같은 슬롯(회원·장소·날짜)에 활성 예약이 있으면:
        //   - PENDING(미결제): 결제하러 갔다가 돌아온 경우이므로 그 예약을 재사용해 결제를 이어가게 한다
        //   - PAID(결제완료): 이미 확정된 예약이므로 새 예약을 거부한다
        Reservation existing = reservationMapper.findActiveBySlot(memberId, req.getPlaceId(), req.getVisitDate());
        if (existing != null) {
            if (existing.getStatus() == ReservationStatus.PENDING) {
                return existing.getReservationId();
            }
            // "마감"이 아니라 본인이 이미 잡아둔 예약이므로, 그 사실이 드러나게 안내한다
            throw new IllegalStateException("이미 예약하신 날짜입니다. 내 예약 내역에서 확인해 주세요.");
        }

        // 무료(freeTest) 오버라이드가 아니면 PLACE.place_type을 조회해 가격 정책을 판단한다
        String placeType = req.isFreeTest() ? "free" : reservationMapper.findPlaceType(req.getPlaceId());

        // 마감 백업 체크(2겹): 숙박만 '1팀 단독'이라 요청 기간과 겹치는 예약이 있으면 거부.
        // 맛집/관광지(만나서결제)는 하루 여러 팀 가능 → 이 체크를 건너뛴다. (프론트 캘린더와 동일 규칙)
        if (isStay(placeType)) {
            if (req.getCheckOutDate() == null) {
                throw new IllegalStateException("체크아웃 날짜를 선택해 주세요.");
            }
            if (!req.getCheckOutDate().isAfter(req.getVisitDate())) {
                throw new IllegalStateException("체크아웃 날짜는 체크인 다음 날부터 선택할 수 있습니다.");
            }
            if (reservationMapper.countOverlapping(req.getPlaceId(), req.getVisitDate(), req.getCheckOutDate()) > 0) {
                throw new IllegalStateException("이미 예약된 기간입니다. 다른 날짜를 선택해 주세요.");
            }
        }

        // 관리자가 날짜별로 마감 지정한 날(PLACE_CLOSED_DATE)이 요청 기간에 포함되면 거부
        LocalDate closedRangeTo = isStay(placeType) ? req.getCheckOutDate() : req.getVisitDate().plusDays(1);
        if (reservationMapper.countClosedDatesInRange(req.getPlaceId(), req.getVisitDate(), closedRangeTo) > 0) {
            throw new IllegalStateException("마감된 날짜가 포함되어 있습니다.");
        }

        // 맛집/관광지 하루 정원(DAILY_CAPACITY) 초과 시 거부. 숙박은 1팀 단독이라 위 overlap 체크로 이미 처리됨
        if (isPayOnSite(placeType)) {
            int already = reservationMapper.sumHeadcountOnDate(req.getPlaceId(), req.getVisitDate());
            if (already + req.getHeadcount() > DAILY_CAPACITY) {
                throw new IllegalStateException("정원이 초과되어 예약할 수 없습니다.");
            }
        }

        Reservation r = new Reservation();
        r.setMemberId(memberId);
        r.setPlaceId(req.getPlaceId());
        r.setVisitorName(req.getVisitorName());
        r.setPhone(req.getPhone());
        r.setVisitDate(req.getVisitDate());
        r.setCheckOutDate(isStay(placeType) ? req.getCheckOutDate() : null);   // 숙박만 기간 예약
        r.setHeadcount(req.getHeadcount());
        r.setStatus(ReservationStatus.PENDING);
        reservationMapper.insert(r);
        return r.getReservationId();
    }

    /** PLACE.place_type 조회(tour/food/stay). 예약 폼 렌더링에 필요 */
    @Transactional(readOnly = true)
    public String getPlaceType(Long placeId) {
        return reservationMapper.findPlaceType(placeId);
    }

    /**
     * 결제 확정 직전 마지막 방어: 예약 생성(PENDING) 이후 결제 대기 중에 관리자가 마감시켰을 수 있으니
     * PaymentService.saveSuccess()에서 PAID로 바꾸기 전에 다시 한 번 확인한다.
     */
    @Transactional(readOnly = true)
    public void assertStillAvailable(Long reservationId) {
        Reservation r = getById(reservationId);
        if (Integer.valueOf(1).equals(reservationMapper.findPlaceClosed(r.getPlaceId()))) {
            throw new IllegalStateException("휴무 중인 장소입니다.");
        }
        LocalDate closedRangeTo = isStay(r.getPlaceType()) ? r.getCheckOutDate() : r.getVisitDate().plusDays(1);
        if (reservationMapper.countClosedDatesInRange(r.getPlaceId(), r.getVisitDate(), closedRangeTo) > 0) {
            throw new IllegalStateException("마감된 날짜가 포함되어 있습니다.");
        }
    }

    /** PLACE.name 조회. 예약 폼 표시용 */
    @Transactional(readOnly = true)
    public String getPlaceName(Long placeId) {
        return reservationMapper.findPlaceName(placeId);
    }

    /** PLACE.first_image 조회. 예약 폼 카드 썸네일 표시용 (다운로드된 이미지가 없으면 null) */
    @Transactional(readOnly = true)
    public String getPlaceImage(Long placeId) {
        return reservationMapper.findPlaceImage(placeId);
    }

    /** PLACE.address 조회. 예약 폼 카드 설명 라인 표시용 */
    @Transactional(readOnly = true)
    public String getPlaceAddress(Long placeId) {
        return reservationMapper.findPlaceAddress(placeId);
    }

    /** 숙박 1인 단가 조회. PLACE.min_price를 읽기 전용으로 참조 */
    @Transactional(readOnly = true)
    public int getUnitPrice(Long placeId) {
        Integer price = reservationMapper.findMinPrice(placeId);
        if (price == null) {
            throw new IllegalStateException("등록된 가격이 없는 장소입니다. placeId=" + placeId);
        }
        return price;
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
     * 반환: { "booked": { "yyyy-MM-dd": 예약인원합, ... }, "closed": 휴무 여부 }
     * 프론트는 booked 에 들어있는 날(=이미 예약된 날)이나 closed=true(장소 전체 휴무)를
     * 마감(빨강·선택불가)으로 그린다. (1일 1팀)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(Long placeId) {
        Map<String, Integer> booked = new LinkedHashMap<>();
        for (Map<String, Object> row : reservationMapper.findActiveRanges(placeId)) {
            LocalDate from = ((Date) row.get("visitDate")).toLocalDate();
            Date rawTo = (Date) row.get("checkOutDate");
            // 체크아웃이 없으면(맛집/관광지 등 당일) 하루만 막고, 있으면 체크아웃 전날까지 막는다
            LocalDate to = (rawTo != null) ? rawTo.toLocalDate() : from.plusDays(1);
            int count = ((Number) row.get("headcount")).intValue();

            for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
                booked.merge(d.toString(), count, Integer::sum);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("booked", booked);
        result.put("closed", Integer.valueOf(1).equals(reservationMapper.findPlaceClosed(placeId)));
        result.put("closedDates", reservationMapper.findClosedDates(placeId));
        result.put("capacity", DAILY_CAPACITY);   // 맛집·관광지 캘린더의 남은 자리 표시용(차단 아님)
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
     * 사업자: 결제완료(PAID) 예약 거절 — 사유 기록만 담당, 환불/상태전환은 PaymentService.rejectPaid가 이어서 처리.
     * reason이 비어있으면 기본 문구로 대체하고 trim해서 저장한다(PaymentService가 PG 취소 사유로도 그대로 재사용하므로
     * DB에 남는 값과 PG에 보내는 값이 항상 같도록 정규화는 여기서 한 번만 한다). 리턴값을 그 정규화된 사유로 준다.
     */
    @Transactional
    public String reject(Long reservationId, String reason) {
        Reservation r = getById(reservationId);
        if (r.getStatus() != ReservationStatus.PAID) {
            throw new IllegalStateException("결제완료된 예약만 거절할 수 있습니다. 현재 상태: " + r.getStatus().getLabel());
        }
        String finalReason = (reason == null || reason.isBlank()) ? "사업자 거절" : reason.trim();
        reservationMapper.reject(reservationId, finalReason);
        return finalReason;
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
     * - 숙박: 박수 × 인원 × 단가(PLACE.min_price)
     */
    public int calculateAmount(Reservation r) {
        if (isPayOnSite(r.getPlaceType())) {
            return RESERVE_DEPOSIT;
        }
        return nightsOf(r) * r.getHeadcount() * getUnitPrice(r.getPlaceId());
    }
}
