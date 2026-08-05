package com.gnagnoohc.travel.reservation.controller;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.service.MypageService;
import com.gnagnoohc.travel.reservation.dto.ReservationCreateRequest;
import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
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
    private final PaymentService paymentService;
    private final MypageService mypageService;

    /** 개발용 테스트 허브. 예약~결제~취소 흐름을 수동으로 확인하는 페이지 */
    @GetMapping("/test")
    public String testPage(Model model) {
        // 개발용 임시 memberId=1. 로그인 연동 후 세션값으로 교체
        model.addAttribute("reservations", reservationService.getMyReservations(1));
        return "reservation/test";
    }

    /**
     * 예약 폼 페이지. 숙박/맛집 상세 페이지에서 /reservations/new?placeId=1 로 진입.
     * placeType(tour/food/stay)은 PLACE 조회로 결정한다 — tour/food면 예약금(만나서결제), stay면 정가.
     * freeTest=true면 0원 즉시완료 테스트 플래그로 취급(예약 테스트 허브 전용, PLACE.place_type엔 없는 값).
     */
    @GetMapping("/new")
    public String form(@RequestParam("placeId") Long placeId,
                       @RequestParam(value = "freeTest", required = false, defaultValue = "false") boolean freeTest,
                       HttpSession session,
                       Model model) {
        // 예약 폼도 로그인 필수 — 예약자 정보 자동입력을 위해 회원 조회가 필요해 폼 진입 자체를 로그인 필수로 전환
        LoginMemberDto loginMember = (LoginMemberDto) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/auth/login";
        }
        MypageDto member = mypageService.getMemberInfo((long) loginMember.getMemberId());
        model.addAttribute("loginMemberName", member.getName());
        model.addAttribute("loginMemberPhone", member.getPhone());

        String placeType = freeTest ? "free" : reservationService.getPlaceType(placeId);
        model.addAttribute("placeId", placeId);
        model.addAttribute("placeType", placeType);
        model.addAttribute("placeName", reservationService.getPlaceName(placeId));
        model.addAttribute("placeImage", reservationService.getPlaceImage(placeId));
        model.addAttribute("placeAddress", reservationService.getPlaceAddress(placeId));
        // 숙박만 min_price를 조회 — 관광지/맛집은 min_price가 항상 null이라 그대로 부르면 예외남
        model.addAttribute("price", ReservationService.isStay(placeType) ? reservationService.getUnitPrice(placeId) : 0);
        model.addAttribute("deposit", ReservationService.RESERVE_DEPOSIT);
        return "reservation/reservationForm";
    }

    /**
     * 예약 캘린더용: 장소의 이미 예약된 날짜 목록 + 휴무 여부.
     * 프론트 캘린더가 이 값으로 "예약된 날/휴무 장소 = 빨강(선택 불가)"을 그린다. (1일 1팀)
     * 응답 예: { "booked": { "2026-07-29": 10, "2026-07-30": 5 }, "closed": false }
     */
    @GetMapping("/availability")
    @ResponseBody
    public Map<String, Object> availability(@RequestParam("placeId") Long placeId) {
        return reservationService.getAvailability(placeId);
    }

    /** 예약 생성 -> 결제 수단 선택 페이지로 이동 */
    @PostMapping
    public String create(@Valid @ModelAttribute ReservationCreateRequest req,
                         BindingResult bindingResult,
                         HttpSession session, Model model) {
        // 서버측 검증 실패(폼 조작/직접 요청 등) 시 폼으로 되돌림 — 프론트 검증의 최종 방어선
        if (bindingResult.hasErrors()) {
            log.warn("[예약 생성 검증 실패] {}", bindingResult.getAllErrors());
            LoginMemberDto sessionMember = (LoginMemberDto) session.getAttribute("loginMember");
            if (sessionMember != null) {
                MypageDto member = mypageService.getMemberInfo((long) sessionMember.getMemberId());
                model.addAttribute("loginMemberName", member.getName());
                model.addAttribute("loginMemberPhone", member.getPhone());
            }
            model.addAttribute("placeId", req.getPlaceId());
            String placeType = req.isFreeTest() ? "free" : reservationService.getPlaceType(req.getPlaceId());
            model.addAttribute("placeType", placeType);
            model.addAttribute("placeName", reservationService.getPlaceName(req.getPlaceId()));
            model.addAttribute("placeImage", reservationService.getPlaceImage(req.getPlaceId()));
            model.addAttribute("placeAddress", reservationService.getPlaceAddress(req.getPlaceId()));
            model.addAttribute("price", ReservationService.isStay(placeType) ? reservationService.getUnitPrice(req.getPlaceId()) : 0);
            model.addAttribute("deposit", ReservationService.RESERVE_DEPOSIT);
            return "reservation/reservationForm";
        }
        // 로그인 세션의 회원 식별. 로그인이 "loginMember"(LoginMemberDto)로 저장하므로 그 memberId를 사용
        LoginMemberDto loginMember = (LoginMemberDto) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/auth/login";
        }
        Integer memberId = loginMember.getMemberId();
        log.info("[예약 생성 요청] memberId={}, {}", memberId, req);
        Long reservationId = reservationService.create(memberId, req);
        log.info("[예약 생성 완료] reservationId={}", reservationId);

        // 결제금액이 0원(무료)이면 결제창을 거치지 않고 곧장 결제완료 처리 → 관리자 승인 대기로 넘어간다
        // freeTest는 PLACE.place_type과 무관한 테스트 전용 오버라이드라 여기서 직접 판단한다
        Reservation r = reservationService.getById(reservationId);
        int amount = req.isFreeTest() ? 0 : reservationService.calculateAmount(r);
        if (amount == 0) {
            String orderId = paymentService.generateOrderId(reservationId);
            String freeKey = "FREE-" + reservationId + "-" + System.currentTimeMillis();
            Payment payment = paymentService.saveSuccess(reservationId, 0, freeKey, orderId, Payment.TYPE_FREE);
            log.info("[무료 예약 즉시 완료] reservationId={}, paymentId={}", reservationId, payment.getPaymentId());
            return "redirect:/payments/complete/" + payment.getPaymentId();
        }

        return "redirect:/payments/checkout/" + reservationId;
    }

    /* ------------------- 취소 요청/승인/거절 ------------------- */

    /** 사용자: 취소 요청 (마이페이지의 취소 요청 모달에서 호출). 사유 전달, 환불은 관리자 승인 시 */
    @PostMapping("/{reservationId}/cancel-request")
    @ResponseBody
    public Map<String, String> cancelRequest(@PathVariable("reservationId") Long reservationId,
                                             @RequestParam(value = "reason", required = false) String reason,
                                             HttpSession session) {
        LoginMemberDto loginMember = (LoginMemberDto) session.getAttribute("loginMember");
        if (loginMember == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        Integer memberId = loginMember.getMemberId();

        reservationService.requestCancel(reservationId, memberId, reason);
        log.info("[취소 요청] reservationId={}, memberId={}, reason={}", reservationId, memberId, reason);
        return Map.of("result", "OK");
    }
}
