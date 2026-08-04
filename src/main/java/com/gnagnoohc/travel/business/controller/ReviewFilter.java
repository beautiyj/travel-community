package com.gnagnoohc.travel.business.controller;

import com.gnagnoohc.travel.business.dto.BusinessReviewSentimentCountsDto;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 후기 확인 화면의 감성 필터 탭 정의.
 * 탭 순서·화면 라벨·조회에 쓸 REVIEW_ANALYSIS.sentiment 값·탭에 표시할 건수를 한 곳에서 함께 정의한다.
 * (ReservationFilter와 같은 구성)
 */
enum ReviewFilter {

    ALL("전체", null, counts -> null),
    POSITIVE("긍정", 1, BusinessReviewSentimentCountsDto::getPositiveCount),
    NEUTRAL("중립", 0, BusinessReviewSentimentCountsDto::getNeutralCount),
    NEGATIVE("부정", -1, BusinessReviewSentimentCountsDto::getNegativeCount);

    private final String label;
    private final Integer sentiment;
    private final Function<BusinessReviewSentimentCountsDto, Integer> countExtractor;

    ReviewFilter(String label, Integer sentiment,
                 Function<BusinessReviewSentimentCountsDto, Integer> countExtractor) {
        this.label = label;
        this.sentiment = sentiment;
        this.countExtractor = countExtractor;
    }

    /** 화면에서 넘어온 한글 라벨 -> REVIEW_ANALYSIS.sentiment 값 (긍정=1, 중립=0, 부정=-1, 전체/미지정=null) */
    static Integer toSentimentValue(String label) {
        return Arrays.stream(values())
                .filter(filter -> filter.label.equals(label))
                .findFirst()
                .map(filter -> filter.sentiment)
                .orElse(null);
    }

    /** 필터 탭에 뿌릴 "라벨 -> 건수" (순서 유지). 전체 탭은 건수를 표시하지 않으므로 null */
    static Map<String, Integer> toTabs(BusinessReviewSentimentCountsDto counts) {
        Map<String, Integer> tabs = new LinkedHashMap<>();
        for (ReviewFilter filter : values()) {
            tabs.put(filter.label, filter.countExtractor.apply(counts));
        }
        return tabs;
    }
}
