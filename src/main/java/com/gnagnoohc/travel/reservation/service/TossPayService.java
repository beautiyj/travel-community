package com.gnagnoohc.travel.reservation.service;

import com.gnagnoohc.travel.reservation.dto.TossConfirmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * 토스페이먼츠 연동.
 * 문서: https://docs.tosspayments.com/guides/v2/payment-widget/integration
 * 흐름: (프론트) 위젯 렌더 → requestPayment() → 결제창 → successUrl 로 리다이렉트
 *      (서버) paymentKey/orderId/amount 수신 → confirm 호출로 최종 승인
 * 주의: successUrl 로 온 amount 를 그대로 믿지 말고, ready 단계에서 세션에 저장해둔
 *      금액과 반드시 비교할 것 (클라이언트가 amount 를 조작해서 보낼 수 있음)
 */
@Slf4j
@Service
public class TossPayService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossConfirmResponse confirm(String paymentKey, String orderId, int amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);

        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        log.info("[토스 confirm 요청] paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);
        TossConfirmResponse res = restClient.post()
                .uri("/v1/payments/confirm")
                .header("Authorization", authHeader)
                // 같은 orderId로 재시도돼도(네트워크 타임아웃 등) 토스가 같은 요청으로 인식하도록 orderId 기반 고정 키 사용
                .header("Idempotency-Key", "confirm-" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TossConfirmResponse.class);
        log.info("[토스 confirm 응답] {}", res);
        return res;
    }

    /** 결제 취소 (전액). tid 대신 paymentKey 사용 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancel(String paymentKey, String cancelReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", cancelReason);

        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        log.info("[토스 cancel 요청] paymentKey={}, reason={}", paymentKey, cancelReason);
        Map<String, Object> res = restClient.post()
                .uri("/v1/payments/" + paymentKey + "/cancel")
                .header("Authorization", authHeader)
                // 같은 paymentKey로 재시도돼도 토스가 같은 요청으로 인식하도록 paymentKey 기반 고정 키 사용
                .header("Idempotency-Key", "cancel-" + paymentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        log.info("[토스 cancel 응답] {}", res);
        return res;
    }

    /** 정합성 보정 배치용: orderId로 결제 상태 조회 (주문조회 GET /v1/payments/orders/{orderId}) */
    public TossConfirmResponse getOrder(String orderId) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        log.info("[토스 주문조회 요청] orderId={}", orderId);
        TossConfirmResponse res = restClient.get()
                .uri("/v1/payments/orders/{orderId}", orderId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(TossConfirmResponse.class);
        log.info("[토스 주문조회 응답] orderId={}, status={}", orderId, res != null ? res.getStatus() : null);
        return res;
    }

    private <T> T post(String uri, Map<String, Object> body, Class<T> type) {
        return null; // 미사용 (확장 대비 자리만)
    }
}