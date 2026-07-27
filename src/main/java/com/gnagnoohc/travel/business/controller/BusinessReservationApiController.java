package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.business.service.BusinessReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessReservationApiController {

    private final BusinessReservationService businessReservationService;

    //예약 마감. memberId는 요청값이 아니라 로그인 세션에서 파생한다 (미로그인 401 / 권한부족 403)
    @PatchMapping("/place/closed")
    public ResponseEntity<Void> setPlaceClosed(
            @RequestParam Long placeId,
            @RequestParam boolean isClosed,
            HttpSession session
    ) {
        LoginMemberDto login = BusinessSessionSupport.getLogin(session);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!BusinessSessionSupport.isBusiness(login)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        businessReservationService.setPlaceClosed(placeId, (long) login.getMemberId(), isClosed);
        return ResponseEntity.ok().build();
    }
}
