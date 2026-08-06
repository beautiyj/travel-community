package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 법정동코드조회 /ldongCode2 전용 DTO - 메타데이터(시스템 상 변동 없는 지정 데이터) 분리

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourLdongCodeDTO {
    // (요청)법정동 목록조회 여부 - N(코드조회)일 때 표출되는 필드
    private String code;            // 법정동 코드 (시도코드, 시군구코드)
    private String name;            // 법정동 명칭 (시도명, 시군구명)

    // (요청)법정동 목록조회 여부 - Y(전체목록조회)일 때 표출되는 필드
    // lDongRegnCd처럼 소문자 1글자+대문자로 시작하는 필드명은 Jackson이 getter/setter 이름으로부터
    // property명을 역추론할 때 Lombok과 인식이 어긋나 매핑이 깨지는 문제가 있어, @JsonProperty로 JSON 키를 명시 고정
    @JsonProperty("lDongRegnCd")
    private String lDongRegnCd;     // 법정동 시도코드

    @JsonProperty("lDongRegnNm")
    private String lDongRegnNm;     // 법정동 시도명

    @JsonProperty("lDongSignguCd")
    private String lDongSignguCd;   // 법정동 시군구코드

    @JsonProperty("lDongSignguNm")
    private String lDongSignguNm;   // 법정동 시군구명
}