package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 예약 관리 화면의 상태 필터 탭 정의.
 * 탭 순서·화면 라벨·조회에 쓸 상태값·탭에 표시할 건수를 한 곳에서 함께 정의해,
 * 라벨이 바뀌거나 상태가 늘어도 이 파일만 고치면 되게 한다.
 * 사업자 조치가 필요한 상태(결제완료=확정/거절 대기, 취소요청)를 앞쪽에 두고 나머지는 예약 진행 순서대로 배치.
 */
enum ReservationFilter {

    ALL("전체", null, counts -> null),
    PAID("결제완료", ReservationStatus.PAID, BusinessReservationStatusCountsDto::getPaidCount),
    CANCEL_REQUESTED("취소요청", ReservationStatus.CANCEL_REQUESTED, BusinessReservationStatusCountsDto::getCancelRequestCount),
    CONFIRMED("확정", ReservationStatus.CONFIRMED, BusinessReservationStatusCountsDto::getConfirmedCount),
    COMPLETED("완료", ReservationStatus.COMPLETED, BusinessReservationStatusCountsDto::getDoneCount),
    CANCELED("취소", ReservationStatus.CANCELED, BusinessReservationStatusCountsDto::getCancelledCount);

    private final String label;
    private final ReservationStatus status;
    private final Function<BusinessReservationStatusCountsDto, Integer> countExtractor;

    ReservationFilter(String label, ReservationStatus status,
                      Function<BusinessReservationStatusCountsDto, Integer> countExtractor) {
        this.label = label;
        this.status = status;
        this.countExtractor = countExtractor;
    }

    /** 화면에서 넘어온 한글 라벨 -> 조회에 쓸 RESERVATION.status 값. 전체/미지정이면 null(=필터 없음) */
    static String toStatusName(String label) {
        return Arrays.stream(values())
                .filter(filter -> filter.label.equals(label))
                .findFirst()
                .map(filter -> filter.status == null ? null : filter.status.name())
                .orElse(null);
    }

    /** 필터 탭에 뿌릴 "라벨 -> 건수" (순서 유지). 전체 탭은 건수를 표시하지 않으므로 null */
    static Map<String, Integer> toTabs(BusinessReservationStatusCountsDto counts) {
        Map<String, Integer> tabs = new LinkedHashMap<>();
        for (ReservationFilter filter : values()) {
            tabs.put(filter.label, filter.countExtractor.apply(counts));
        }
        return tabs;
    }
}
