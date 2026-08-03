package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.dto.BusinessPlaceDto;

public interface BusinessService {

    BusinessApplicationDto getApplication(Long memberId);

    void resubmit(Long memberId, BusinessApplicationDto application);

    List<BusinessPlaceDto> getPlaces(Long memberId);

    void addPlaceImage(Long memberId, Long placeId, String imageUrl);
}
