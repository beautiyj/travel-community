package com.gnagnoohc.travel.admin.controller;

import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import com.gnagnoohc.travel.admin.service.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReservationApiController {

    private final AdminReservationService adminReservationService;

    // TODO: 로그인/세션 붙으면 memberId 파라미터 제거하고 인증 정보에서 가져오기
    @GetMapping("/reservations")
    public List<AdminReservationDto> getReservations(
            @RequestParam Long placeId,
            @RequestParam Long memberId,
            @RequestParam(required = false) String status
    ) {
        return adminReservationService.getReservations(placeId, memberId, status);
    }

    @PostMapping("/reservations/{reservationId}/accept")
    public void accept(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.accept(reservationId, memberId);
    }

    @PostMapping("/reservations/{reservationId}/reject")
    public void reject(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.reject(reservationId, memberId);
    }

    @PatchMapping("/place/closed")
    public void setPlaceClosed(
            @RequestParam Long placeId,
            @RequestParam Long memberId,
            @RequestParam boolean isClosed
    ) {
        adminReservationService.setPlaceClosed(placeId, memberId, isClosed);
    }
}
