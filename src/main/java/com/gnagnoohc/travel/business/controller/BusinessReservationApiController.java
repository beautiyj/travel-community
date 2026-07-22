package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.business.service.BusinessReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessReservationApiController {

    private final BusinessReservationService businessReservationService;

    //예약 마감
    @PatchMapping("/place/closed")
    public void setPlaceClosed(
            @RequestParam Long placeId,
            @RequestParam Long memberId,
            @RequestParam boolean isClosed
    ) {
        businessReservationService.setPlaceClosed(placeId, memberId, isClosed);
    }
}
