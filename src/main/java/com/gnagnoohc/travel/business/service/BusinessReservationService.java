package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessReservationService {

    private final BusinessMapper businessMapper;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    public List<BusinessReservationDto> getReservations(Long placeId, Long bizMemberId, String status) {
        return businessMapper.selectReservationsByPlace(placeId, bizMemberId, status);
    }

    public BusinessReservationStatusCountsDto getStatusCounts(Long placeId, Long bizMemberId) {
        return businessMapper.selectReservationStatusCounts(placeId, bizMemberId);
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

    /** 마감 날짜 추가. 지난 날짜는 막고, 이미 등록된 날짜는 그대로 성공 처리한다(멱등). */
    public void addClosedDate(Long placeId, Long bizMemberId, LocalDate closedDate) {
        if (closedDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("지난 날짜는 마감할 수 없습니다.");
        }
        if (businessMapper.selectClosedDates(placeId, bizMemberId).contains(closedDate)) {
            return;
        }
        if (businessMapper.insertClosedDate(placeId, bizMemberId, closedDate) == 0) {
            throw new IllegalArgumentException("해당 업소가 없거나 처리 권한이 없습니다.");
        }
    }

    public void removeClosedDate(Long placeId, Long bizMemberId, LocalDate closedDate) {
        if (businessMapper.deleteClosedDate(placeId, bizMemberId, closedDate) == 0) {
            throw new IllegalArgumentException("해당 마감 날짜가 없거나 처리 권한이 없습니다.");
        }
    }
}
