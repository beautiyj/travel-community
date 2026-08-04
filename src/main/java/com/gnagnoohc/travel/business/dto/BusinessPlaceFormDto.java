package com.gnagnoohc.travel.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 업소 등록/수정 폼(venueFormFields.jsp)의 입력값 묶음.
 * 등록과 수정이 같은 입력 필드를 쓰므로 하나로 받아 서비스까지 그대로 넘긴다.
 * (사진은 MultipartFile이라 별도 파라미터로 받는다)
 */
@Getter
@Setter
public class BusinessPlaceFormDto {
    private String name;
    private String placeType;
    private String priceType;
    private Integer minPrice;
    private Long regionId;
    private String address;
    private String description;
    private String hashtags;

    /**
     * 부가정보 입력칸. 라벨과 값이 같은 순서로 들어오는 병렬 리스트다.
     * (라벨은 hidden input, 값은 옆의 text input이라 폼에서 항상 짝을 이뤄 제출된다.
     *  선택하지 않은 업종의 입력칸은 JS가 disabled 처리해 아예 전송되지 않는다)
     * BusinessExtraInfoCatalog.assemble()이 "[라벨] 값" 한 덩어리로 조립해 extra_info에 저장한다.
     */
    private List<String> extraLabels;
    private List<String> extraValues;
}
