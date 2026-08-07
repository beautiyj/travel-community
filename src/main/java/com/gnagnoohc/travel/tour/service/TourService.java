package com.gnagnoohc.travel.tour.service;

import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourMapper tourMapper;

    private static final int PAGE_SIZE = 16;   // 카드 4열 x 4줄
    private static final int PAGE_BLOCK = 10;  // 페이지 번호 블록 단위 (1~10, 11~20)

    // 통합 검색용 keyword + sort(정렬) 파라미터가 포함된 목록 조회
    public List<PlaceDTO> getPlaceList(String placeType, Integer regionId, String keyword, String sort, int page) {
        // 페이지 범위 검증 - 재하는 총 페이지 수를 초과하여 요청했을 때 방어하기 위해 1부터 totalPages 사이의 안전한 범위로 보정
        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;
        return tourMapper.selectPlaceList(placeType, regionId, keyword, sort, offset, PAGE_SIZE);
    }

    // Controller 전용: 장소 목록 + 페이징 블록(startPage, endPage 등) + 정렬을 한 번에 계산하여 반환
    public Map<String, Object> getPlacePage(String placeType, Integer regionId, String keyword, String sort, int page) {
        int totalCount = tourMapper.countPlaceList(placeType, regionId, keyword);
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);

        int currentPage = Math.max(1, Math.min(page, Math.max(totalPages, 1)));
        int offset = (currentPage - 1) * PAGE_SIZE;
        
        List<PlaceDTO> placeList = tourMapper.selectPlaceList(placeType, regionId, keyword, sort, offset, PAGE_SIZE);

        int startPage = ((currentPage - 1) / PAGE_BLOCK) * PAGE_BLOCK + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK - 1, totalPages);

        Map<String, Object> result = new HashMap<>();
        result.put("placeList", placeList);
        result.put("totalCount", totalCount);
        result.put("page", currentPage);
        result.put("totalPages", totalPages);
        result.put("startPage", startPage);
        result.put("endPage", endPage);
        
        return result;
    }

    // 상위 시/도 지역 목록 조회 (지역 필터 버튼 바 동적 생성용)
    public List<RegionDTO> getParentRegionList() {
        return tourMapper.selectParentRegions();
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

        // http 프로토콜을 https로 강제 변환 (Mixed Content 브라우저 보안 차단 방지)
        images.forEach(img -> {
            if (img.getImageUrl() != null && img.getImageUrl().startsWith("http://")) {
                img.setImageUrl(img.getImageUrl().replace("http://", "https://"));
            }
        });

        // 대표 이미지(firstImage) 프로토콜 변환 및 공백 제거
        String firstImgUrl = (place != null && place.getFirstImage() != null)
                ? place.getFirstImage().replace("http://", "https://").trim() 
                : null;
        
        // 대표 이미지(firstImage)와 URL이 완전히 같은 항목 필터링 및 중복 제거
        List<PlaceImageDTO> filteredImages = images.stream()
                .filter(img -> firstImgUrl == null || !firstImgUrl.equalsIgnoreCase(img.getImageUrl().trim()))
                .distinct()
                .toList();

        // 만약 대표 이미지와 중복된 걸 빼서 리스트가 비더라도, 원본 슬라이드는 나올 수 있도록 방어 처리
        return filteredImages.isEmpty() ? images : filteredImages;
    }

    // 부가정보를 줄바꿈 기준("\n")으로 미리 쪼개서 리스트로 반환해주는 편의 메서드
    public List<String> getExtraInfoLines(String extraInfo) {
        if (extraInfo == null || extraInfo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 자바에서는 '\n' 또는 '\r\n'을 안전하게 쪼갤 수 있도록 정규식으로 처리
        return Arrays.asList(extraInfo.split("\\r?\\n"));
    }
}