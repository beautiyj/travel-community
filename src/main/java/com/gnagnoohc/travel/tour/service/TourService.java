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

    // 장소 이미지 목록 조회 (대표 이미지와 중복되는 항목 제거 및 https 프로토콜 보정)
    public List<PlaceImageDTO> getPlaceImages(Integer placeId) {
        PlaceDTO place = tourMapper.selectPlaceById(placeId);
        List<PlaceImageDTO> images = tourMapper.getImagesByPlaceId(placeId);
        
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. http 프로토콜을 https로 강제 변환 (Mixed Content 브라우저 보안 차단 방지)
        images.forEach(img -> {
            if (img.getImageUrl() != null && img.getImageUrl().startsWith("http://")) {
                img.setImageUrl(img.getImageUrl().replace("http://", "https://"));
            }
        });

        // 2. 대표 이미지(firstImage) 프로토콜 변환 및 공백 제거
        String firstImgUrl = (place != null && place.getFirstImage() != null) 
                ? place.getFirstImage().replace("http://", "https://").trim() 
                : null;
        
        // 3. 대표 이미지(firstImage)와 URL이 완전히 같은 항목 필터링 및 중복 제거
        List<PlaceImageDTO> filteredImages = images.stream()
                .filter(img -> firstImgUrl == null || !firstImgUrl.equalsIgnoreCase(img.getImageUrl().trim()))
                .distinct()
                .toList();

        // 만약 대표 이미지와 중복된 걸 빼서 리스트가 비더라도, 원본 슬라이드는 나올 수 있도록 방어 처리
        return filteredImages.isEmpty() ? images : filteredImages;
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