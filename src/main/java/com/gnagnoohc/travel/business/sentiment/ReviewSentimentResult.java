package com.gnagnoohc.travel.business.sentiment;

import java.util.List;

// sentiment: -1 부정 / 0 중립 / 1 긍정
public record ReviewSentimentResult(int sentiment, List<KeywordCount> keywords) {
}
