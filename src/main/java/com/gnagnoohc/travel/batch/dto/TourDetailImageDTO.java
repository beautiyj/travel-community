package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 이미지정보조회 /detailImage2 전용 DTO

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourDetailImageDTO {
    @NotBlank(message = "contentid: 콘텐츠ID는 필수 수집 항목입니다.") private String contentid;
    
    private String imgname;        // 이미지명
    private String originimgurl;   // 원본이미지 URL (약 500*333 size)
    private String smallimageurl;  // 썸네일이미지 URL (약 160*100 size)
    private String serialnum;      // 이미지 일련번호 (1대 N 판별 기준)
    private String cpyrhtDivCd;    // 저작권 유형 (Type1:제1유형(출처표시-권장), Type3:제3유형(제1유형+변경금지))
}
