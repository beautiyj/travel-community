package com.gnagnoohc.travel.batch.payment.adapter;

import com.gnagnoohc.travel.reservation.entity.Reservation;

/**
 * "이 주문, PG에서 실제로 결제됐어?"를 묻는 조회 규격.
 * 토스/카카오(추후 무통장/가상카드)가 각자 이 인터페이스를 구현하고,
 * 공통 정합성 보정 서비스는 이 인터페이스만 알 뿐 구체 PG는 모른다.
 *
 * <p>Phase 1(토대)에서는 규격만 정의하고 구현체는 두지 않는다.
 * 실제 조회 로직(TossInquiryAdapter/KakaoInquiryAdapter)과 이를 소비하는
 * 배치 서비스는 Phase 2에서 추가한다.
 */
public interface PaymentInquiryAdapter {

    /** 내가 처리할 결제수단인지 (Payment.TYPE_TOSS / TYPE_KAKAO 등) */
    boolean supports(int paymentType);

    /** 대기 중인 예약 1건을 PG에 조회해 실제 결제 상태를 반환. r.getOrderId()를 조회 키로 쓴다. */
    InquiryResult inquire(Reservation r);
}
