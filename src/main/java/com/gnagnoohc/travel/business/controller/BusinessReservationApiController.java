package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.business.service.BusinessReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    /* ------------------- 날짜별 예약 마감 ------------------- */

    @GetMapping("/place/closed-dates")
    public ResponseEntity<List<LocalDate>> getClosedDates(@RequestParam Long placeId, HttpSession session) {
        LoginMemberDto login = requireBusinessLogin(session);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                businessReservationService.getClosedDates(placeId, (long) login.getMemberId()));
    }

    @PostMapping("/place/closed-dates")
    public ResponseEntity<Void> addClosedDate(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closedDate,
            HttpSession session
    ) {
        LoginMemberDto login = requireBusinessLogin(session);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        businessReservationService.addClosedDate(placeId, (long) login.getMemberId(), closedDate);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/place/closed-dates")
    public ResponseEntity<Void> removeClosedDate(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closedDate,
            HttpSession session
    ) {
        LoginMemberDto login = requireBusinessLogin(session);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        businessReservationService.removeClosedDate(placeId, (long) login.getMemberId(), closedDate);
        return ResponseEntity.ok().build();
    }

    // 로그인/권한 확인 공통 처리. 통과하면 로그인 정보를, 아니면 null을 돌려준다.
    // (기존 setPlaceClosed는 401/403을 구분해 응답하므로 그대로 두었다)
    private LoginMemberDto requireBusinessLogin(HttpSession session) {
        LoginMemberDto login = BusinessSessionSupport.getLogin(session);
        return (login != null && BusinessSessionSupport.isBusiness(login)) ? login : null;
    }
}
