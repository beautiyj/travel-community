package com.gnagnoohc.travel.mypage.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.dto.BusinessPlaceDto;

@Mapper
public interface BusinessRepository {

    BusinessApplicationDto findApplicationByMemberId(Long memberId);

    int resubmitApplication(BusinessApplicationDto application);

    List<BusinessPlaceDto> findPlacesByMemberId(Long memberId);

    int countOwnedPlace(@Param("placeId") Long placeId,
                        @Param("memberId") Long memberId);

    int insertPlaceImage(@Param("placeId") Long placeId,
                         @Param("imageUrl") String imageUrl);
}
