package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.dto.ReservationCreateRequest;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 임시 1인 단가. 숙박/맛집 파트의 가격 컬럼이 확정되면 그쪽 조회로 교체 */
    public static final int TEMP_UNIT_PRICE = 10000;

    private final ReservationMapper reservationMapper;

    /** 예약 생성 (결제 전이므로 '예약중' 상태). 같은 장소·날짜 중복 예약은 거부 */
    @Transactional
    public Long create(Long memberId, ReservationCreateRequest req) {
        // 슬롯 선점: 이미 활성 예약이 있으면 선점 실패
        int active = reservationMapper.countActive(memberId, req.getPlaceId(), req.getVisitDate());
        if (active > 0) {
            throw new IllegalStateException("해당 날짜에 이미 예약이 있습니다.");
        }

        Reservation r = new Reservation();
        r.setMemberId(memberId);
        r.setPlaceId(req.getPlaceId());
        r.setVisitorName(req.getVisitorName());
        r.setPhone(req.getPhone());
        r.setVisitDate(req.getVisitDate());
        r.setHeadcount(req.getHeadcount());
        r.setStatus(Reservation.STATUS_PENDING);
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
    public List<Reservation> getMyReservations(Long memberId) {
        return reservationMapper.findByMemberId(memberId);
    }

    @Transactional
    public void updateStatus(Long reservationId, String status) {
        reservationMapper.updateStatus(reservationId, status);
    }

    /**
     * 취소 요청 (결제완료 건). 상태를 '취소요청'으로 바꾸고 사유 기록. 환불은 관리자 승인 시 실행.
     * 본인 예약 + '예약완료' 상태에서만 가능.
     */
    @Transactional
    public void requestCancel(Long reservationId, Long memberId, String reason) {
        Reservation r = getById(reservationId);
        if (!r.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 예약만 취소 요청할 수 있습니다.");
        }
        if (!Reservation.STATUS_PAID.equals(r.getStatus())) {
            throw new IllegalStateException("결제완료된 예약만 취소 요청할 수 있습니다. 현재 상태: " + r.getStatus());
        }
        reservationMapper.requestCancel(reservationId, reason);
    }

    /**
     * 관리자: 취소 요청 거절. '취소요청' → '예약완료'로 원복 (환불 없음).
     */
    @Transactional
    public void rejectCancel(Long reservationId) {
        Reservation r = getById(reservationId);
        if (!Reservation.STATUS_CANCEL_REQUESTED.equals(r.getStatus())) {
            throw new IllegalStateException("취소 요청 상태인 예약만 거절할 수 있습니다. 현재 상태: " + r.getStatus());
        }
        reservationMapper.updateStatus(reservationId, Reservation.STATUS_PAID);
    }

    /** 관리자 목록용: 특정 상태의 예약 조회 */
    @Transactional(readOnly = true)
    public List<Reservation> getByStatus(String status) {
        return reservationMapper.findByStatus(status);
    }

    /**
     * 결제 금액 계산.
     * TODO: 숙박/맛집 팀원의 place(가격) 테이블이 나오면 placeId로 단가 조회하도록 교체
     */
    public int calculateAmount(Reservation r) {
        return r.getHeadcount() * TEMP_UNIT_PRICE;
    }
}
