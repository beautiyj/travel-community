package com.gnagnoohc.travel.tour.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.batch.dto.TourItemDTO;

@Mapper
public interface TourMapper {

    // 공공데이터 수집 후 PLACE 테이블에 알박기(UPSERT)용 메서드
    void upsertPlace(TourItemDTO item);
    
}
