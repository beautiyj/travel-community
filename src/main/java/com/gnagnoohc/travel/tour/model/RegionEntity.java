package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegionEntity {
    private Long regionId;
    private String regionName;
    private Long parentRegionId;

    /*
    MyBatis가 DB 조회 결과를 엔티티/DTO에 매핑(resultType 또는 resultMap)할 때
    생성자를 거치지 않고 기본 생성자(@NoArgsConstructor)로 객체를 만든 뒤
    Reflection(리플렉션)을 이용해 필드에 직접 값을 채워넣기 때문에 커스텀 생성자 모두 삭제, @NoArgsConstructor 처리
    */

}