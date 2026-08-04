package com.gnagnoohc.travel.reservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 토스페이먼츠 결제 승인(confirm) 응답 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossConfirmResponse {

    private String paymentKey;
    private String orderId;
    private String method;        // "카드", "가상계좌" 등
    private String approvedAt;
    private int totalAmount;
    private String status;        // "DONE", "CANCELED", "ABORTED", "EXPIRED" 등 — 주문조회(getOrder) 응답에도 동일하게 포함됨
}