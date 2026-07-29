package com.gnagnoohc.travel.business.sentiment;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 규칙기반 후기 감성분석 : Komoran 형태소분석 + KNU 감성사전 매칭
@Component
@RequiredArgsConstructor
public class ReviewSentimentAnalyzer {

    // 명사 키워드에서 제외할 불용어 (2단계 워드클라우드에서도 재사용)
    private static final Set<String> NOUN_STOPWORDS = Set.of(
            "이", "그", "저", "것", "수", "때", "곳", "등", "및", "때문", "정도", "저희", "우리", "여기"
    );

    private final SentimentDictionary dictionary;
    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    public ReviewSentimentResult analyze(String content) {
        if (content == null || content.isBlank()) {
            return new ReviewSentimentResult(0, List.of());
        }

        KomoranResult result = komoran.analyze(content);

        int score = 0;
        Map<String, Integer> keywordCounts = new LinkedHashMap<>();

        for (Token token : result.getTokenList()) {
            String morph = token.getMorph();
            String pos = token.getPos();

            score += scoreOf(morph, pos);

            if (("NNG".equals(pos) || "NNP".equals(pos)) && morph.length() >= 2 && !NOUN_STOPWORDS.contains(morph)) {
                keywordCounts.merge(morph, 1, Integer::sum);
            }
        }

        List<KeywordCount> keywords = new ArrayList<>();
        keywordCounts.forEach((word, count) -> keywords.add(new KeywordCount(word, count)));
        keywords.sort((a, b) -> b.count() - a.count());

        return new ReviewSentimentResult(Integer.compare(score, 0), keywords);
    }

    // KNU 사전은 용언을 종결형으로 등재해서(예: "좋다","깨끗하다"), 형태소 어간 그대로가 아니라
    // 어미를 복원한 형태로 사전을 조회해야 매칭된다 (좋(VA)->좋다, 깨끗(XR)->깨끗하다)
    private int scoreOf(String morph, String pos) {
        List<String> candidates = new ArrayList<>();
        if (pos.startsWith("VV") || pos.startsWith("VA")) {
            candidates.add(morph + "다");
        } else if ("XR".equals(pos)) {
            candidates.add(morph + "하다");
            candidates.add(morph + "다");
        } else {
            candidates.add(morph);
        }

        for (String candidate : candidates) {
            Integer polarity = dictionary.polarityOf(candidate);
            if (polarity != null) {
                return polarity;
            }
        }
        return 0;
    }
}
