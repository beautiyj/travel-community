package com.gnagnoohc.travel.business.dto;

import com.gnagnoohc.travel.business.sentiment.KeywordCount;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BusinessReviewSentimentCountsDto {
    private int positiveCount;
    private int negativeCount;
    private int neutralCount;
    private List<KeywordCount> keywords; // 워드클라우드용 상위 키워드 (2단계)
}
