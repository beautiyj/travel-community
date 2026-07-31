package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.dto.BusinessPlaceDto;

public interface BusinessService {

    BusinessApplicationDto getApplication(Long memberId);

    void submitApplication(
            Long memberId, BusinessApplicationDto application);

    void cancelPendingApplication(Long memberId);

    List<BusinessPlaceDto> getPlaces(Long memberId);
}
