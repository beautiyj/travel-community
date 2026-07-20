package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 반복정보조회 /detailInfo2 전용 DTO

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourDetailInfoDTO {
    @NotBlank(message = "contentid: 콘텐츠ID는 필수 수집 항목입니다.") private String contentid;
    @NotBlank(message = "contenttypeid: 관광타입ID는 필수 수집 항목입니다.") private String contenttypeid;

    // [일반 타입 공통 - 숙박, 여행코스 제외]
    private String fldgubun;               // 일련번호
    private String infoname;               // 제목 (예: 입장료, 이용시간 등)
    private String infotext;               // 내용 (예: 무료, 09:00~18:00 등)
    private String serialnum;              // 반복일련번호

    // [여행코스 타입 - contentTypeId=25]
    private String subcontentid;           // 하위콘텐츠ID
    private String subname;                // 코스명
    private String subnum;                 // 반복일련번호
    private String subdetailoverview;      // 코스개요
    private String subdetailimg;           // 코스이미지
    private String subdetailalt;           // 코스이미지설명

    // [숙박 타입 - contentTypeId=32]
    private String roomcode;               // 객실코드
    private String roomtitle;              // 객실명칭
    private String roomcount;              // 객실수
    private String roomsize1;              // 객실크기(평)
    private String roomsize2;              // 객실크기(평방미터)
    private String roombasecount;          // 기준인원
    private String roommaxcount;           // 최대인원
    private String roomintro;              // 객실소개
    
    // 숙박 요금 관련
    private String roomoffseasonminfee1;   // 비수기주중최소요금
    private String roomoffseasonminfee2;   // 비수기주말최소요금
    private String roompeakseasonminfee1;  // 성수기주중최소요금
    private String roompeakseasonminfee2;  // 성수기주말최소요금

    // 숙박 주요 시설 (자잘한 가전제품/세면도구는 쳐내고 핵심만 유지)
    private String roomaircondition;       // 에어컨여부
    private String roomtv;                 // TV 여부
    private String roominternet;           // 인터넷여부
    private String roomrefrigerator;       // 냉장고여부
    private String roomcook;               // 취사용품여부
    private String roombathfacility;       // 목욕시설여부

    // 숙박 객실 사진 (대표 사진 1~2개 위주로 검토할 수 있게 배치)
    private String roomimg1;               // 객실사진1
    private String roomimg1alt;            // 객실사진1 설명
    private String cpyrhtDivCd1;           // 저작권 유형 1
    private String roomimg2;               // 객실사진2
    private String roomimg2alt;            // 객실사진2 설명
    private String cpyrhtDivCd2;           // 저작권 유형 2
}
