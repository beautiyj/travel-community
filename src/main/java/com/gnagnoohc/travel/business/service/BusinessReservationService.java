package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
