package com.gnagnoohc.travel.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

// 공공데이터 JSON 응답 형태의 공통 껍데기 DTO - 헤더바디아이템아이템 구조의 중첩 JSON 공통 형태
// 응답객체 전체(가장 최상위 JSON 데이터 덩어리. 상자 개념)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiResponseDto<T> {
 
    @JsonProperty("response")
    private Response<T> response;

    // JSON 데이터의 헤더/바디 영역 구분
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response<T> {
        private Header header;
        private Body<T> body;
    }

    // 헤더 영역 - 객체 2개 구분(성공유무 결과코드, 결과 메시지 매핑 - 헤더 실패 시 메타데이터도 안들어옴)
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    // 바디 영역 - 아이템 구분(메타데이터 + 알맹이 주머니 형태. 페이징 정보 등과 실제 데이터 구분용도)
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body<T> {
        private Items<T> items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }

    // 바디 안의 아이템 안 실제데이터 구분
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items<T> {
        // 단건/다건 방어 코드(다건 - 정상 리스트 형태 / 단건 - 단일 객체 형태)
        // @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) : 단건 시 요소가 1개인 리스트 생성하기
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<T> item;
    }
}
