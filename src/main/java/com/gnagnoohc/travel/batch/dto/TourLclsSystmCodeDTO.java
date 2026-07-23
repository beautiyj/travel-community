package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 분류체계 코드조회 /lclsSystmCode2 전용 DTO - 메타데이터(시스템 상 변동 없는 지정 데이터) 분리

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourLclsSystmCodeDTO {
    private String rnum;            // 일련번호

    // (요청)분류체계 목록조회 여부 - N(코드조회)일 때 표출되는 필드
    private String code;            // 대/중/소분류 코드  (1Depth,2Depth,3Depth)
    private String name;            // 대/중/소분류 코드명(1Depth,2Depth,3Depth 코드명)

    // (요청)분류체계 목록조회 여부 - Y(전체목록조회)일 때 표출되는 필드
    private String lclsSystm1Cd;    // 분류체계 대분류코드
    private String lclsSystm1Nm;    // 분류체계 대분류명
    private String lclsSystm2Cd;    // 분류체계 중분류코드
    private String lclsSystm2Nm;    // 분류체계 중분류명
    private String lclsSystm3Cd;    // 분류체계 소분류코드
    private String lclsSystm3Nm;    // 분류체계 소분류명
}