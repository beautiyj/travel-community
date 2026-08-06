package com.gnagnoohc.travel.tour.mapper;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TourMapper {

    // PLACE 테이블 UPSERT
    void upsertPlace(PlaceDTO place);
    
    // REGION 테이블 UPSERT
    void upsertRegion(RegionDTO region);

    // PLACE_IMAGE 테이블 INSERT
    void insertPlaceImage(PlaceImageDTO image);

    // PLACE 조회 (ResultMap/ResultType -> PlaceDTO 반환)
    PlaceDTO selectPlaceById(Integer placeId);

    // 지역 코드 기반 장소 목록 조회 (TourService의 selectByAreaCode 에러 해결용)
    List<com.gnagnoohc.travel.tour.model.PlaceEntity> selectByAreaCode(@Param("areaCode") String areaCode);

    // 키워드 파라미터 추가 - 타입별 및 지역별 장소 목록 통합 조회 (TourController의 getPlaceList 에러 해결용)
    List<PlaceDTO> selectPlaceList(
            @Param("placeType") String placeType,
            @Param("regionId") Integer regionId,
            @Param("keyword") String keyword
    );
    
    List<PlaceImageDTO> getImagesByPlaceId(@Param("placeId") Integer placeId);

    // 특정 지역의 현재 적재된 PLACE 건수 조회 (지역별 무작위 샘플링 쿼터 체크용)
    int selectPlaceCountByRegion(@Param("regionId") Integer regionId);

    // 스케줄러 적용 로직 2가지
    int countPlacesByRegionAndType(@Param("regionId") String regionId, @Param("placeType") String placeType);
    List<RegionDTO> selectAllRegions();

    List<RegionDTO> selectParentRegions();

}
