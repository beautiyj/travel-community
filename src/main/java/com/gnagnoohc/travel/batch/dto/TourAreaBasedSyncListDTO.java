package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 관광정보 동기화 목록 조회 /areaBasedSyncList2
// 배치 수집 전용 API (DB 최신상태 유지용 API) 로직 DTO라서 분리

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourAreaBasedSyncListDTO {
    @NotBlank(message = "contentid: 콘텐츠ID는 필수 수집 항목입니다.") private String contentid;
    @NotBlank(message = "contenttypeid: 관광타입ID는 필수 수집 항목입니다.") private String contenttypeid;
    @NotBlank(message = "createdtime: 등록일은 필수 수집 항목입니다.") private String createdtime;
    @NotBlank(message = "modifiedtime: 콘텐츠 수정일은 필수 수집 항목입니다.") private String modifiedtime;
    @NotBlank(message = "title: 제목은 필수 수집 항목입니다.") private String title;
    @NotBlank(message = "showflag: 표출여부는 동기화 필수 항목입니다.") private String showflag; 

    private String addr1;
    private String addr2;
    private String firstimage;
    private String firstimage2;
    private String cpyrhtDivCd;
    private String mapx;
    private String mapy;
    private String mlevel;
    private String tel;
    private String zipcode;
    
    // 분류 및 법정동 코드
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
}
