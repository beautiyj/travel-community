package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.business.exception.BusinessAuthException;
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
            @RequestParam Integer isClosed,
            HttpSession session
    ) {
        Long bizMemberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessReservationService.setPlaceClosed(placeId, bizMemberId, isClosed);
        return ResponseEntity.ok().build();
    }

    /* ------------------- 날짜별 예약 마감 ------------------- */

    @GetMapping("/place/closed-dates")
    public ResponseEntity<List<LocalDate>> getClosedDates(@RequestParam Long placeId, HttpSession session) {
        Long bizMemberId = BusinessSessionSupport.requireBusinessMemberId(session);
        return ResponseEntity.ok(businessReservationService.getClosedDates(placeId, bizMemberId));
    }

    // endDate를 안 보내면 하루짜리 마감(startDate=endDate)으로 처리한다.
    @PostMapping("/place/closed-dates")
    public ResponseEntity<Void> addClosedDates(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session
    ) {
        Long bizMemberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessReservationService.addClosedDateRange(placeId, bizMemberId, startDate, endOrStart(startDate, endDate));
        return ResponseEntity.ok().build();
    }

    // 목록에서 하루짜리 행이든 묶인 구간 행이든 같은 방식으로 지운다 (endDate 생략 시 하루만).
    @DeleteMapping("/place/closed-dates")
    public ResponseEntity<Void> removeClosedDates(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session
    ) {
        Long bizMemberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessReservationService.removeClosedDateRange(placeId, bizMemberId, startDate, endOrStart(startDate, endDate));
        return ResponseEntity.ok().build();
    }

    // 마감 구간 수정: 기존 구간(oldStartDate~oldEndDate)을 지우고 새 구간(newStartDate~newEndDate)으로 교체한다.
    @PutMapping("/place/closed-dates")
    public ResponseEntity<Void> updateClosedDates(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate oldStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate oldEndDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate,
            HttpSession session
    ) {
        Long bizMemberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessReservationService.updateClosedDateRange(
                placeId, bizMemberId, oldStartDate, oldEndDate, newStartDate, newEndDate);
        return ResponseEntity.ok().build();
    }

    // 종료일을 생략하면 하루짜리 구간으로 본다
    private static LocalDate endOrStart(LocalDate startDate, LocalDate endDate) {
        return endDate != null ? endDate : startDate;
    }

    // 미로그인 401 / 사업자 아님 403. 화면 컨트롤러는 같은 예외를 리다이렉트로 처리한다.
    @ExceptionHandler(BusinessAuthException.class)
    public ResponseEntity<Void> handleBusinessAuth(BusinessAuthException e) {
        HttpStatus status = e.getReason() == BusinessAuthException.Reason.NOT_LOGGED_IN
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).build();
    }
}
