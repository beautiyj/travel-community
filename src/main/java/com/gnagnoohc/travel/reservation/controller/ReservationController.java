package com.gnagnoohc.travel.reservation.controller;

import com.gnagnoohc.travel.reservation.dto.ReservationCreateRequest;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 개발용 테스트 허브. 예약~결제~취소 흐름을 수동으로 확인하는 페이지 */
    @GetMapping("/test")
    public String testPage(Model model) {
        // 개발용 임시 memberId=1. 로그인 연동 후 세션값으로 교체
        model.addAttribute("reservations", reservationService.getMyReservations(1L));
        return "reservation/test";
    }

    /** 예약 폼 페이지. 숙박/맛집 상세 페이지에서 /reservations/new?placeId=1 로 진입 */
    @GetMapping("/new")
    public String form(@RequestParam("placeId") Long placeId, Model model) {
        model.addAttribute("placeId", placeId);
        // TODO: 숙박/맛집 파트의 place 조회가 나오면 실제 단가로 교체
        model.addAttribute("price", ReservationService.TEMP_UNIT_PRICE);
        return "reservation/reservationForm";
    }

    /** 예약 생성 -> 결제 수단 선택 페이지로 이동 */
    @PostMapping
    public String create(@ModelAttribute ReservationCreateRequest req, HttpSession session) {
        // TODO: 로그인 파트와 연동 - 세션에 저장되는 회원 키 이름을 팀 컨벤션에 맞추기
        Long memberId = (Long) session.getAttribute("loginMemberId");
        if (memberId == null) {
            memberId = 1L;   // 개발용 임시값. 로그인 연동 후 제거
        }
        log.info("[예약 생성 요청] memberId={}, {}", memberId, req);
        Long reservationId = reservationService.create(memberId, req);
        log.info("[예약 생성 완료] reservationId={}", reservationId);
        return "redirect:/payments/checkout/" + reservationId;
    }

    /* ------------------- 취소 요청/승인/거절 ------------------- */

    /** 사용자: 취소 요청 (마이페이지의 취소 요청 모달에서 호출). 사유 전달, 환불은 관리자 승인 시 */
    @PostMapping("/{reservationId}/cancel-request")
    @ResponseBody
    public Map<String, String> cancelRequest(@PathVariable("reservationId") Long reservationId,
                                             @RequestParam(value = "reason", required = false) String reason,
                                             HttpSession session) {
        Long memberId = (Long) session.getAttribute("loginMemberId");
        if (memberId == null) memberId = 1L;   // 개발용 임시값. 로그인 연동 후 제거

        reservationService.requestCancel(reservationId, memberId, reason);
        log.info("[취소 요청] reservationId={}, memberId={}, reason={}", reservationId, memberId, reason);
        return Map.of("result", "OK");
    }
}
