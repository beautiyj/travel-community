package com.gnagnoohc.travel.business.sentiment;

// 후기 명사 키워드 빈도. REVIEW_ANALYSIS.keywords(JSON)로 저장되며 워드클라우드에서 사용
public record KeywordCount(String word, int count) {
}
