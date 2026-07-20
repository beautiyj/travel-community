package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 응답 구조 비슷한 공공데이터는 아이템 DTO 하나로 묶기
// DTO 분리 기준: 응답 필드 중 순번/일련번호들이 들어있을 경우(하위 리스트라서 분리)
//               구조가 1:1이 아닌 1:N 구조 형태일 때(받아오는 데이터가 리스트 형태일 경우 분리)
//               메인 식별자가 2개 이상 묶여야할 때(EX 상세정보 매핑은 contentid와 순번까지 같이 확인해야 데이터 매핑 가능)

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourItemDTO {
    // 필수 항목
    @NotBlank(message = "contentid: 콘텐츠ID는 필수 수집 항목입니다.") private String contentid;
    @NotBlank(message = "contenttypeid: 관광타입ID는 필수 수집 항목입니다.") private String contenttypeid;
    @NotBlank(message = "createdtime: 등록일은 필수 수집 항목입니다.") private String createdtime;
    @NotBlank(message = "modifiedtime: 콘텐츠 수정일은 필수 수집 항목입니다.") private String modifiedtime;
    @NotBlank(message = "title: 제목(콘텐츠명)은 필수 수집 항목입니다.") private String title;

    // locationBasedList2 필수 항목
    @NotNull(message = "중심좌표로부터의 거리는 필수 수집 항목입니다.")
    private Double dist; // 필수(1)이면서 숫자 기반 데이터이므로 Double 처리

    // 옵션 항목(0) - 공공데이터 사정에 따라 null일 수 있음
    private String addr1;       // 주소
    private String addr2;       // 상세주소
    private String firstimage;  // 대표이미지(원본)
    private String firstimage2; // 대표이미지(썸네일) - 테이블에도 썸네일용 별도 컬럼 생성 필요
    private String cpyrhtDivCd; // 저작권 유형 - Type1:제1유형(출처표시-권장) Type3:제3유형(제1유형 + 변경금지)
    private String mapx;        // 경도
    private String mapy;        // 위도
    private String mlevel;      // Map Level 응답
    private String tel;         // 전화번호
    private String zipcode;     // 우편번호
    
    private String overview;    // 개요(콘텐츠개요조회)

    // 법정동 및 분류체계 코드 데이터
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;

    // 반려동물 동반 데이터
    private String acmpyPsblCpam;       // 동반가능동물
    private String relaRntlPrdlst;      // 관련 렌탈 품목
    private String acmpyNeedMtr;        // 동반시 필요사항
    private String relaFrnshPrdlst;     // 관련 비치 품목
    private String etcAcmpyInfo;        // 기타 동반 정보(주의사항)
    private String relaAcdntRiskMtr;    // 관련 사고 대비사항
    private String acmpyTypeCd;         // 동반유형코드(동반구분)
    private String relaPosesFclty;      // 관련 구비 시설
    private String petTursmInfo;        // 반려동물 관광정보
}