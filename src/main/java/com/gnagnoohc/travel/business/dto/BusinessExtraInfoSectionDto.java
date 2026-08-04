package com.gnagnoohc.travel.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 부가정보 항목 고르기(칩) 목록을 화면에서 묶어 보여줄 단위.
 *
 * 관광지(tour)는 place_type 하나로 관광지/문화시설/레포츠를 함께 쓰는데, 명세서상 6개 라벨은
 * 세부유형 전용이다. 세부유형은 DB에 저장하지 않으므로 저장값에는 영향을 주지 않고
 * 화면에서 소제목으로만 구분해 사업자가 자기 업장에 맞는 항목만 고르도록 돕는다.
 * 숙박/음식점은 세부 구분이 없어 소제목 없는 섹션 하나로 내려간다.
 */
@Getter
@AllArgsConstructor
public class BusinessExtraInfoSectionDto {
    // 비어 있으면 소제목 없이 칩만 렌더링한다
    private String title;
    private List<BusinessExtraInfoOptionDto> options;
}
