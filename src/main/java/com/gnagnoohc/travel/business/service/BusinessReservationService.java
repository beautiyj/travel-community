package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessReservationService {

    private final BusinessMapper businessMapper;

    public List<BusinessReservationDto> getReservations(Long placeId, Long bizMemberId, String status) {
        return businessMapper.selectReservationsByPlace(placeId, bizMemberId, status);
    }

    public BusinessReservationStatusCountsDto getStatusCounts(Long placeId, Long bizMemberId) {
        return businessMapper.selectReservationStatusCounts(placeId, bizMemberId);
    }

    public void accept(Long reservationId, Long bizMemberId) {
        changeStatus(reservationId, bizMemberId, "확정");
    }

    public void reject(Long reservationId, Long bizMemberId) {
        changeStatus(reservationId, bizMemberId, "취소");
    }

    private void changeStatus(Long reservationId, Long bizMemberId, String status) {
        int updated = businessMapper.updateReservationStatus(reservationId, bizMemberId, status);
        if (updated == 0) {
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
