package com.gnagnoohc.travel.business.sentiment;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// KNU 한국어 감성사전(resources/sentiment/SentiWord_Dict.txt) 로딩. 단어 -> 극성 점수(-2~2)
@Component
public class SentimentDictionary {

    private final Map<String, Integer> polarityByWord = new HashMap<>();

    @PostConstruct
    void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("sentiment/SentiWord_Dict.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split("\t");
                if (cols.length != 2) {
                    continue;
                }
                try {
                    polarityByWord.put(cols[0], Integer.parseInt(cols[1].trim()));
                } catch (NumberFormatException ignored) {
                    // 사전 파일의 형식이 어긋난 줄은 건너뜀
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("KNU 감성사전 로딩 실패", e);
        }
    }

    public Integer polarityOf(String word) {
        return polarityByWord.get(word);
    }
}
