package com.gnagnoohc.travel.business.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.business.dto.BusinessReviewDto;
import com.gnagnoohc.travel.business.dto.BusinessReviewSentimentCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import com.gnagnoohc.travel.business.sentiment.KeywordCount;
import com.gnagnoohc.travel.business.sentiment.ReviewSentimentAnalyzer;
import com.gnagnoohc.travel.business.sentiment.ReviewSentimentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

// 후기 감성분석 : MVP는 실시간(글쓰기 시점) 분석 대신, 대시보드 조회 시점에 그동안 쌓인
// 미분석 후기만 골라 분석/저장한다 (커뮤니티 글쓰기 API를 건드리지 않기 위함, 이미 분석된 후기는 재분석하지 않음)
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSentimentService {

    // 워드클라우드에 노출할 상위 키워드 개수
    private static final int TOP_KEYWORD_LIMIT = 40;

    private final BusinessMapper businessMapper;
    private final ReviewSentimentAnalyzer analyzer;
    private final ObjectMapper objectMapper;

    // 대시보드용 : 긍정/부정/중립 건수 + 전체 후기 키워드를 합산한 상위 키워드(워드클라우드용)
    public BusinessReviewSentimentCountsDto getSentimentSummary(Long placeId) {
        analyzeUnanalyzed(placeId);

        BusinessReviewSentimentCountsDto summary = businessMapper.selectSentimentCounts(placeId);
        summary.setKeywords(aggregateTopKeywords(placeId));
        return summary;
    }

    private void analyzeUnanalyzed(Long placeId) {
        List<BusinessReviewDto> unanalyzed = businessMapper.selectUnanalyzedReviews(placeId);

        for (BusinessReviewDto review : unanalyzed) {
            ReviewSentimentResult result = analyzer.analyze(review.getContent());
            String keywordsJson = toJson(result.keywords());
            businessMapper.insertReviewAnalysis(review.getPostId(), placeId, result.sentiment(), keywordsJson);
        }
    }

    // 후기별로 저장된 keywords(JSON)를 모두 읽어 같은 단어끼리 빈도를 합산한다
    private List<KeywordCount> aggregateTopKeywords(Long placeId) {
        List<String> keywordsJsonList = businessMapper.selectAnalyzedKeywordsJsonByPlace(placeId);

        Map<String, Integer> merged = new LinkedHashMap<>();
        for (String json : keywordsJsonList) {
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                List<KeywordCount> keywords = objectMapper.readValue(json, new TypeReference<List<KeywordCount>>() {
                });
                for (KeywordCount keyword : keywords) {
                    merged.merge(keyword.word(), keyword.count(), Integer::sum);
                }
            } catch (Exception e) {
                log.warn("후기 키워드 JSON 파싱 실패: {}", json, e);
            }
        }

        return merged.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(TOP_KEYWORD_LIMIT)
                .map(e -> new KeywordCount(e.getKey(), e.getValue()))
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("후기 키워드 JSON 직렬화 실패", e);
            return "[]";
        }
    }
}
