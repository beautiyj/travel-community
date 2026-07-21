package com.gnagnoohc.travel.reservation.controller;

import com.gnagnoohc.travel.reservation.dto.KakaoApproveResponse;
import com.gnagnoohc.travel.reservation.dto.KakaoReadyResponse;
import com.gnagnoohc.travel.reservation.entity.Payment;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import com.gnagnoohc.travel.reservation.service.KakaoPayService;
import com.gnagnoohc.travel.reservation.service.PaymentService;
import com.gnagnoohc.travel.reservation.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final KakaoPayService kakaoPayService;

    /* ------------------- 결제 수단 선택 (부모 창) ------------------- */
    @GetMapping("/checkout/{reservationId}")
    public String checkout(@PathVariable("reservationId") Long reservationId, Model model) {
        Reservation r = reservationService.getById(reservationId);
        model.addAttribute("reservation", r);
        model.addAttribute("amount", reservationService.calculateAmount(r));
        return "reservation/checkout";
    }

    /* ------------------- 카카오 (팝업) ------------------- */
    @PostMapping("/kakao/ready/{reservationId}")
    @ResponseBody
    public Map<String, String> kakaoReady(@PathVariable("reservationId") Long reservationId,
                                          HttpSession session) {
        Reservation r = reservationService.getById(reservationId);
        int amount = reservationService.calculateAmount(r);
        String orderId = paymentService.generateOrderId(reservationId);

        KakaoReadyResponse ready = kakaoPayService.ready(r, amount, orderId);

        session.setAttribute("KAKAO_TID", ready.getTid());
        session.setAttribute("KAKAO_ORDER_ID", orderId);
        session.setAttribute("KAKAO_AMOUNT", amount);
        session.setAttribute("KAKAO_MEMBER_ID", r.getMemberId());

        return Map.of("redirectUrl", ready.getNextRedirectPcUrl());
    }

    @GetMapping("/kakao/success")
    public String kakaoSuccess(@RequestParam("reservationId") Long reservationId,
                               @RequestParam("pg_token") String pgToken,
                               HttpSession session, Model model) {
        String tid = (String) session.getAttribute("KAKAO_TID");
        String orderId = (String) session.getAttribute("KAKAO_ORDER_ID");
        Integer amount = (Integer) session.getAttribute("KAKAO_AMOUNT");
        Long memberId = (Long) session.getAttribute("KAKAO_MEMBER_ID");

        log.info("[카카오 success 콜백] reservationId={}, 세션값 tid={}, orderId={}, amount={}",
                reservationId, tid, orderId, amount);

        if (tid == null || orderId == null || amount == null) {
            return bridgeToFail(model, "결제 정보가 만료되었습니다. 다시 시도해 주세요.");
        }

        KakaoApproveResponse approve = kakaoPayService.approve(tid, orderId, memberId, pgToken);
        Payment payment = paymentService.saveSuccess(
                reservationId, amount, approve.getTid(), orderId, Payment.TYPE_KAKAO);

        session.removeAttribute("KAKAO_TID");
        session.removeAttribute("KAKAO_ORDER_ID");
        session.removeAttribute("KAKAO_AMOUNT");
        session.removeAttribute("KAKAO_MEMBER_ID");

        model.addAttribute("target", "/payments/complete/" + payment.getPaymentId());
        return "reservation/paymentBridge";
    }

    /** 결제창에서 취소하고 나온 경우: 실패 페이지가 아니라 결제하기 페이지로 복귀 */
    @GetMapping("/kakao/cancel")
    public String kakaoCancel(@RequestParam("reservationId") Long reservationId, Model model) {
        model.addAttribute("target", "/payments/checkout/" + reservationId);
        return "reservation/paymentBridge";
    }

    @GetMapping("/kakao/fail")
    public String kakaoFail(Model model) {
        return bridgeToFail(model, "카카오페이 결제에 실패했습니다.");
    }

    /* ------------------- 결제 취소(환불) ------------------- */

    /** 결제 취소: 마이페이지/완료 페이지의 취소 버튼에서 호출 */
    @PostMapping("/{paymentId}/cancel")
    @ResponseBody
    public Map<String, String> cancelPayment(@PathVariable("paymentId") Long paymentId,
                                             @RequestParam(value = "reason", required = false) String reason) {
        paymentService.cancel(paymentId, reason);
        return Map.of("result", "OK");
    }

    /* ------------------- 최종 도착 페이지 ------------------- */
    @GetMapping("/complete/{paymentId}")
    public String complete(@PathVariable("paymentId") Long paymentId, Model model) {
        Payment payment = paymentService.getById(paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("method", "카카오페이");
        return "reservation/success";
    }

    @GetMapping("/failed")
    public String failed(@RequestParam(value = "message", required = false) String message,
                         Model model) {
        model.addAttribute("message", message != null ? message : "결제에 실패했습니다.");
        return "reservation/fail";
    }

    private String bridgeToFail(Model model, String message) {
        String encoded;
        try {
            encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            encoded = "";
        }
        model.addAttribute("target", "/payments/failed?message=" + encoded);
        return "reservation/paymentBridge";
    }
}
