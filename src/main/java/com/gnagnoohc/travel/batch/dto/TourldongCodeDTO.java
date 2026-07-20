package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 법정동코드조회 /ldongCode2 전용 DTO - 메타데이터(시스템 상 변동 없는 지정 데이터) 분리

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourldongCodeDTO {
    private String rnum;            // 일련번호
    
    // (요청)법정동 목록조회 여부 - N(코드조회)일 때 표출되는 필드
    private String code;            // 법정동 코드 (시도코드, 시군구코드)
    private String name;            // 법정동 명칭 (시도명, 시군구명)

    // (요청)법정동 목록조회 여부 - Y(전체목록조회)일 때 표출되는 필드
    private String lDongRegnCd;     // 법정동 시도코드
    private String lDongRegnNm;     // 법정동 시도명
    private String lDongSignguCd;   // 법정동 시군구코드
    private String lDongSignguNm;   // 법정동 시군구명
}
