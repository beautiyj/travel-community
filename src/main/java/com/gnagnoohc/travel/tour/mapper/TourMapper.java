package com.gnagnoohc.travel.tour.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;

@Mapper
public interface TourMapper {

    // PLACE 테이블 UPSERT
    void upsertPlace(PlaceDTO place);
    
    // REGION 테이블 UPSERT
    void upsertRegion(RegionDTO region);

    // PLACE_IMAGE 테이블 INSERT
    void insertPlaceImage(PlaceImageDTO image);

    // PLACE 조회 (ResultMap/ResultType -> PlaceDTO 반환)
    PlaceDTO selectPlaceById(Long placeId);
}
