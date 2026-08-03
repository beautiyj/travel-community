package com.gnagnoohc.travel.tour.service;

import java.util.Arrays; // 1. 임포트 추가
import java.util.Collections; // 1. 임포트 추가
import java.util.List;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceEntity;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourMapper tourMapper;
    
    public List<PlaceEntity> getPlacesByArea(String areaCode) {    
        return tourMapper.selectByAreaCode(areaCode);
    }

    public List<PlaceDTO> getPlaceList(String placeType, Integer regionId, int page) {
        return tourMapper.selectPlaceList(placeType, regionId);
    }

    // 장소 상세 정보 조회
    public PlaceDTO getPlaceDetail(Long placeId) {
        return tourMapper.selectPlaceById(placeId); 
    }

    // 장소 이미지 목록 조회
    public List<PlaceImageDTO> getPlaceImages(Long placeId) {
        return tourMapper.getImagesByPlaceId(placeId);
    }

    // 💡 [추가] 부가정보를 줄바꿈 기준("\n")으로 미리 쪼개서 리스트로 반환해주는 편의 메서드
    public List<String> getExtraInfoLines(String extraInfo) {
        if (extraInfo == null || extraInfo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 자바에서는 '\n' 또는 '\r\n'을 안전하게 쪼갤 수 있습니다.
        return Arrays.asList(extraInfo.split("\\r?\\n"));
    }
}