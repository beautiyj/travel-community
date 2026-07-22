package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReviewDto;
import com.gnagnoohc.travel.business.dto.BusinessReviewSentimentCountsDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessReviewService {

    private final BusinessMapper businessMapper;
    private final ReviewSentimentService reviewSentimentService;

    // sentiment: null=전체, 1=긍정, 0=중립, -1=부정. 필터 정확도를 위해 미분석 후기를 먼저 분석해둔다
    public List<BusinessReviewDto> getReviews(Long placeId, Integer sentiment) {
        reviewSentimentService.getSentimentSummary(placeId);
        return businessMapper.selectReviewsByPlace(placeId, sentiment);
    }

    // 필터 탭에 표시할 긍정/중립/부정 건수
    public BusinessReviewSentimentCountsDto getSentimentCounts(Long placeId) {
        return reviewSentimentService.getSentimentSummary(placeId);
    }
}
