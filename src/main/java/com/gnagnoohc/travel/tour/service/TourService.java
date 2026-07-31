package com.gnagnoohc.travel.tour.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.tour.mapper.TourMapper;
import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourMapper tourMapper;
    
    public List<PlaceEntity> getPlacesByArea(String areaCode) {    
        return tourMapper.selectByAreaCode(areaCode);
    }

    // 컨트롤러에서 호출하는 getPlaceList 메서드 정의
    public List<PlaceDTO> getPlaceList(String placeType, Integer regionId, int page) {
        return tourMapper.selectPlaceList(placeType, regionId);
    }
}