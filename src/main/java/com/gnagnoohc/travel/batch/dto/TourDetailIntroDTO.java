package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 소개정보조회 /detailIntro2 전용 DTO

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourDetailIntroDTO {
    @NotBlank(message = "contentid: 콘텐츠ID는 필수 수집 항목입니다.") private String contentid;
    @NotBlank(message = "contenttypeid: 관광타입ID는 필수 수집 항목입니다.") private String contenttypeid;

    // 옵션 0
    // [관광지 - contentTypeId=12]
    private String accomcount;             // 수용인원
    private String chkbabycarriage;        // 유모차대여정보
    private String chkcreditcard;          // 신용카드가능정보
    private String chkpet;                 // 애완동물동반가능정보
    private String infocenter;             // 문의및안내
    private String opendate;               // 개장일
    private String parking;                // 주차시설
    private String restdate;               // 쉬는날
    private String useseason;              // 이용시기
    private String usetime;                // 이용시간

    // [문화시설 - contentTypeId=14] => 관광지로 분류 예정
    private String accomcountculture;      // 수용인원
    private String chkbabycarriageculture; // 유모차대여정보
    private String chkcreditcardculture;   // 신용카드가능정보
    private String chkpetculture;          // 애완동물동반가능정보
    private String discountinfo;           // 할인정보
    private String infocenterculture;      // 문의및안내
    private String parkingculture;         // 주차시설
    private String parkingfee;             // 주차요금
    private String restdateculture;        // 쉬는날
    private String usefee;                 // 이용요금
    private String usetimeculture;         // 이용시간
    private String spendtime;              // 관람소요시간

    // [여행코스 - contentTypeId=25] => 관광지로 분류 예정 or 데이터 확인 후 제거
    private String distance;               // 코스총거리
    private String infocentertourcourse;   // 문의및안내
    private String schedule;               // 코스일정
    private String taketime;               // 코스총소요시간
    private String theme;                  // 코스테마

    // [레포츠 - contentTypeId=28] => 레저 여부 미정(삭제될 수 있음)
    private String accomcountleports;      // 수용인원
    private String chkbabycarriageleports; // 유모차대여정보
    private String chkcreditcardleports;   // 신용카드가능정보
    private String chkpetleports;          // 애완동물동반가능정보
    private String infocenterleports;      // 문의및안내
    private String openperiod;             // 개장기간
    private String parkingfeeleports;      // 주차요금
    private String parkingleports;         // 주차시설
    private String reservation;            // 예약안내
    private String restdateleports;        // 쉬는날
    private String scaleleports;           // 규모
    private String usefeeleports;          // 입장료
    private String usetimeleports;         // 이용시간

    // [숙박 - contentTypeId=32]
    private String accomcountlodging;      // 수용가능인원
    private String checkintime;            // 입실시간
    private String checkouttime;           // 퇴실시간
    private String foodplace;              // 식음료장
    private String infocenterlodging;      // 문의및안내
    private String parkinglodging;         // 주차시설
    private String roomcount;              // 객실수
    private String reservationlodging;     // 예약안내
    private String roomtype;               // 객실유형
    private String subfacility;            // 부대시설 (기타)

    // [음식점 - contentTypeId=39]
    private String chkcreditcardfood;      // 신용카드가능정보
    private String discountinfofood;       // 할인정보
    private String firstmenu;              // 대표메뉴
    private String infocenterfood;         // 문의및안내
    private String kidsfacility;           // 어린이놀이방여부
    private String opendatefood;           // 개업일
    private String opentimefood;           // 영업시간
    private String packing;                // 포장가능
    private String parkingfood;            // 주차시설
    private String reservationfood;        // 예약안내
    private String restdatefood;           // 쉬는날
    private String seat;                   // 좌석수
    private String treatmenu;              // 취급메뉴
}
