package com.gnagnoohc.travel.admin.service;

import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import com.gnagnoohc.travel.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReservationService {

    private final AdminMapper adminMapper;

    public List<AdminReservationDto> getReservations(Long placeId, Long bizMemberId, String status) {
        return adminMapper.selectReservationsByPlace(placeId, bizMemberId, status);
    }

    public void accept(Long reservationId, Long bizMemberId) {
        changeStatus(reservationId, bizMemberId, "확정");
    }

    public void reject(Long reservationId, Long bizMemberId) {
        changeStatus(reservationId, bizMemberId, "취소");
    }

    private void changeStatus(Long reservationId, Long bizMemberId, String status) {
        int updated = adminMapper.updateReservationStatus(reservationId, bizMemberId, status);
        if (updated == 0) {
            throw new IllegalArgumentException("해당 예약이 없거나 처리 권한이 없습니다.");
        }
    }

    public void setPlaceClosed(Long placeId, Long bizMemberId, boolean isClosed) {
        int updated = adminMapper.updatePlaceClosed(placeId, bizMemberId, isClosed);
        if (updated == 0) {
            throw new IllegalArgumentException("해당 업소가 없거나 처리 권한이 없습니다.");
        }
    }
}
