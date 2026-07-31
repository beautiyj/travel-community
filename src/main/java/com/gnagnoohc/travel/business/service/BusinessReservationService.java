package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessReservationService {

    // 기간별 마감 한 번에 등록 가능한 최대 일수. 상한이 없으면 종료일을 몇 년 뒤로 잡아
    private static final int MAX_CLOSED_RANGE_DAYS = 90;

    private final BusinessMapper businessMapper;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    public List<BusinessReservationDto> getReservations(Long placeId, Long bizMemberId, String status) {
        return businessMapper.selectReservationsByPlace(placeId, bizMemberId, status);
    }

    public BusinessReservationStatusCountsDto getStatusCounts(Long placeId, Long bizMemberId) {
        return businessMapper.selectReservationStatusCounts(placeId, bizMemberId);
    }

    /** 결제완료(PAID) 예약을 확정(CONFIRMED)으로 전환 (예약 파트 소유 로직, 여기서는 소유자 확인만 담당) */
    public void confirm(Long reservationId, Long bizMemberId) {
        requireOwner(reservationId, bizMemberId);
        reservationService.approve(reservationId);
    }

    /**
     * 결제완료(PAID) 예약 거절 → 환불 실행.
     * reservation 파트의 PaymentService.cancel(paymentId, reason)은 Payment.status만 확인하고
     * Reservation.status는 보지 않으므로, 확정/이용완료된 예약까지 환불되지 않도록
     * PAID 검증을 여기서 직접 한다 (approve()가 승인 전에 하는 검증과 같은 역할).
     * reason은 사업자가 입력한 거절 사유. 비어있으면 기본 문구로 대체한다.
     */
    @Transactional
    public void reject(Long reservationId, Long bizMemberId, String reason) {
        requireOwner(reservationId, bizMemberId);

        Reservation reservation = reservationService.getById(reservationId);
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new IllegalStateException(
                    "결제완료된 예약만 거절할 수 있습니다. 현재 상태: " + reservation.getStatus().getLabel());
        }

        Long paymentId = businessMapper.selectDonePaymentId(reservationId, bizMemberId);
        if (paymentId == null) {
            throw new IllegalStateException("환불할 결제완료 내역이 없습니다.");
        }
        String finalReason = (reason == null || reason.isBlank()) ? "사업자 거절" : reason.trim();
        paymentService.cancel(paymentId, finalReason);
    }

    /** 취소 요청 승인 → 카카오 환불 실행 + CANCELED 전환 (예약 파트 소유 로직, 여기서는 소유자 확인만 담당) */
    public void approveCancel(Long reservationId, Long bizMemberId) {
        requireOwner(reservationId, bizMemberId);
        paymentService.approveCancel(reservationId);
    }

    /** 취소 요청 거절 → PAID 원복 (예약 파트 소유 로직, 여기서는 소유자 확인만 담당) */
    public void rejectCancel(Long reservationId, Long bizMemberId) {
        requireOwner(reservationId, bizMemberId);
        reservationService.rejectCancel(reservationId);
    }

    private void requireOwner(Long reservationId, Long bizMemberId) {
        if (!businessMapper.existsReservationForBizMember(reservationId, bizMemberId)) {
            throw new IllegalArgumentException("해당 예약이 없거나 처리 권한이 없습니다.");
        }
    }

    public void setPlaceClosed(Long placeId, Long bizMemberId, boolean isClosed) {
        int updated = businessMapper.updatePlaceClosed(placeId, bizMemberId, isClosed);
        if (updated == 0) {
            throw new IllegalArgumentException("해당 업소가 없거나 처리 권한이 없습니다.");
        }
    }

    /* ------------------- 날짜별 예약 마감 ------------------- */

    public List<LocalDate> getClosedDates(Long placeId, Long bizMemberId) {
        return businessMapper.selectClosedDates(placeId, bizMemberId);
    }

    /*
     * 기간별 마감 추가. */
    @Transactional
    public void addClosedDateRange(Long placeId, Long bizMemberId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("지난 날짜는 마감할 수 없습니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (rangeDays > MAX_CLOSED_RANGE_DAYS) {
            throw new IllegalArgumentException("한 번에 설정할 수 있는 마감 기간은 최대 " + MAX_CLOSED_RANGE_DAYS + "일입니다.");
        }

        List<LocalDate> existing = businessMapper.selectClosedDates(placeId, bizMemberId);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (existing.contains(date)) {
                continue;
            }
            if (businessMapper.insertClosedDate(placeId, bizMemberId, date) == 0) {
                throw new IllegalArgumentException("해당 업소가 없거나 처리 권한이 없습니다.");
            }
        }
    }

    /** 하루 삭제도 startDate=endDate로 넘어온다 (목록의 그룹 행 하나를 통째로 지우는 경우 포함) */
    public void removeClosedDateRange(Long placeId, Long bizMemberId, LocalDate startDate, LocalDate endDate) {
        if (businessMapper.deleteClosedDateRange(placeId, bizMemberId, startDate, endDate) == 0) {
            throw new IllegalArgumentException("해당 마감 날짜가 없거나 처리 권한이 없습니다.");
        }
    }

    /**
     * 마감 구간 수정.*/
    @Transactional
    public void updateClosedDateRange(Long placeId, Long bizMemberId,
                                       LocalDate oldStart, LocalDate oldEnd,
                                       LocalDate newStart, LocalDate newEnd) {
        removeClosedDateRange(placeId, bizMemberId, oldStart, oldEnd);
        addClosedDateRange(placeId, bizMemberId, newStart, newEnd);
    }
}
