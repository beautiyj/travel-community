package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.dto.KakaoApproveResponse;
import com.gnagnoohc.travel.reservation.dto.KakaoReadyResponse;
import com.gnagnoohc.travel.reservation.entity.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오페이 연동 (open-api 신버전).
 * 문서: https://developers.kakaopay.com/docs/payment/online/single-payment
 * 흐름: ready -> next_redirect_pc_url 리다이렉트 -> 사용자 결제 -> pg_token 수신 -> approve
 * 취소: cancel (tid 기준 전액 취소)
 */
@Slf4j
@Service
public class KakaoPayService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://open-api.kakaopay.com")
            .build();

    @Value("${kakaopay.secret-key}")
    private String secretKey;       // DEV 시크릿 키

    @Value("${kakaopay.cid}")
    private String cid;             // 테스트용 단건결제 CID: TC0ONETIME

    @Value("${app.base-url}")
    private String baseUrl;

    public KakaoReadyResponse ready(Reservation r, int amount, String orderId) {
        Map<String, Object> body = new HashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", orderId);
        body.put("partner_user_id", String.valueOf(r.getMemberId()));
        body.put("item_name", "여행 예약 #" + r.getReservationId());
        body.put("quantity", 1);
        body.put("total_amount", amount);
        body.put("tax_free_amount", 0);
        body.put("approval_url", baseUrl + "/payments/kakao/success?reservationId=" + r.getReservationId());
        body.put("cancel_url", baseUrl + "/payments/kakao/cancel?reservationId=" + r.getReservationId());
        body.put("fail_url", baseUrl + "/payments/kakao/fail?reservationId=" + r.getReservationId());

        log.info("[카카오 ready 요청] reservationId={}, orderId={}, amount={}",
                r.getReservationId(), orderId, amount);
        KakaoReadyResponse res = post("/online/v1/payment/ready", body, KakaoReadyResponse.class);
        log.info("[카카오 ready 응답] {}", res);
        return res;
    }

    public KakaoApproveResponse approve(String tid, String orderId, Long memberId, String pgToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("partner_order_id", orderId);
        body.put("partner_user_id", String.valueOf(memberId));
        body.put("pg_token", pgToken);

        log.info("[카카오 approve 요청] tid={}, orderId={}", tid, orderId);
        KakaoApproveResponse res = post("/online/v1/payment/approve", body, KakaoApproveResponse.class);
        log.info("[카카오 approve 응답] {}", res);
        return res;
    }

    /** 결제 취소 (전액) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancel(String tid, int cancelAmount) {
        Map<String, Object> body = new HashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("cancel_amount", cancelAmount);
        body.put("cancel_tax_free_amount", 0);

        log.info("[카카오 cancel 요청] tid={}, cancelAmount={}", tid, cancelAmount);
        Map<String, Object> res = post("/online/v1/payment/cancel", body, Map.class);
        log.info("[카카오 cancel 응답] {}", res);
        return res;
    }

    private <T> T post(String uri, Map<String, Object> body, Class<T> type) {
        return restClient.post()
                .uri(uri)
                .header("Authorization", "SECRET_KEY " + secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
    }
}
