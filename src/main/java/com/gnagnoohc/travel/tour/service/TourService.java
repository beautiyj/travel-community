package com.gnagnoohc.travel.tour.service;

import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceEntity;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourMapper tourMapper;
    
    public List<PlaceEntity> getPlacesByArea(String areaCode) {    
        return tourMapper.selectByAreaCode(areaCode);
    }

    // 0803 통합 검색용 keyword 파라미터 추가
    public List<PlaceDTO> getPlaceList(String placeType, Integer regionId, String keyword, int page) {
        return tourMapper.selectPlaceList(placeType, regionId, keyword);
    }

    // 장소 상세 정보 조회
    public PlaceDTO getPlaceDetail(Integer placeId) {
        return tourMapper.selectPlaceById(placeId); 
    }

    // 장소 이미지 목록 조회
    public List<PlaceImageDTO> getPlaceImages(Integer placeId) {
        return tourMapper.getImagesByPlaceId(placeId);
    }

    // 0803 부가정보를 줄바꿈 기준("\n")으로 미리 쪼개서 리스트로 반환해주는 편의 메서드
    public List<String> getExtraInfoLines(String extraInfo) {
        if (extraInfo == null || extraInfo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 자바에서는 '\n' 또는 '\r\n'을 안전하게 쪼갤 수 있습니다.
        return Arrays.asList(extraInfo.split("\\r?\\n"));
    }
}