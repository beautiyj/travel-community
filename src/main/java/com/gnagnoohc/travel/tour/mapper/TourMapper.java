package com.gnagnoohc.travel.tour.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    // 0731지역 코드 기반 장소 목록 조회 (TourService의 selectByAreaCode 에러 해결용)
    List<com.gnagnoohc.travel.tour.model.PlaceEntity> selectByAreaCode(@Param("areaCode") String areaCode);

    // 0731 타입별 및 지역별 장소 목록 통합 조회 (TourController의 getPlaceList 에러 해결용)
    List<PlaceDTO> selectPlaceList(@Param("placeType") String placeType, @Param("regionId") Integer regionId);

    
    List<PlaceImageDTO> getImagesByPlaceId(@Param("placeId") Long placeId);
}
