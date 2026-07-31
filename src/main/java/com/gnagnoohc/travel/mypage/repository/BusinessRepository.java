package com.gnagnoohc.travel.mypage.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.dto.BusinessPlaceDto;

@Mapper
public interface BusinessRepository {

    BusinessApplicationDto findApplicationByMemberId(Long memberId);

    int saveApplication(BusinessApplicationDto application);

    int deletePendingApplication(Long memberId);

    List<BusinessPlaceDto> findPlacesByMemberId(Long memberId);
}
