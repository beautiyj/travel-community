package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class BusinessReviewDto {

    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private Long postId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;      // 작성자(방문자) 닉네임
    private Integer readcount;    // 조회수 (post.readcount)

    public String getCreatedAtLabel() {
        return createdAt == null ? "" : createdAt.format(DATE_LABEL_FORMAT);
    }
}
