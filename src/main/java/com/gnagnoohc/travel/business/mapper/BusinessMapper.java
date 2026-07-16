package com.gnagnoohc.travel.business.mapper;

import com.gnagnoohc.travel.business.dto.BusinessDashboardCountsDto;
import com.gnagnoohc.travel.business.dto.BusinessMonthlyTrendDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceDetailDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceRegisterDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceUpdateDto;
//import com.gnagnoohc.travel.business.dto.BusinessRegionOptionDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessMapper {

    BusinessPlaceOverviewDto selectPlaceOverviewByMember(@Param("bizMemberId") Long bizMemberId);

    List<BusinessReservationDto> selectTodayReservations(@Param("placeId") Long placeId);

    BusinessDashboardCountsDto selectDashboardCounts(@Param("placeId") Long placeId);

    List<BusinessMonthlyTrendDto> selectMonthlyTrend(@Param("placeId") Long placeId);

    List<BusinessReservationDto> selectReservationsByPlace(
            @Param("placeId") Long placeId,
            @Param("bizMemberId") Long bizMemberId,
            @Param("status") String status
    );

    int updateReservationStatus(
            @Param("reservationId") Long reservationId,
            @Param("bizMemberId") Long bizMemberId,
            @Param("status") String status
    );

    int updatePlaceClosed(
            @Param("placeId") Long placeId,
            @Param("bizMemberId") Long bizMemberId,
            @Param("isClosed") boolean isClosed
    );

//    List<BusinessRegionOptionDto> selectRegionOptions();

    String selectMemberRole(@Param("memberId") Long memberId);

    int insertOwnerPlace(BusinessPlaceRegisterDto place);

    int insertPlaceImage(
            @Param("placeId") Long placeId,
            @Param("imageUrl") String imageUrl,
            @Param("sortOrder") int sortOrder
    );

    BusinessPlaceDetailDto selectPlaceDetailByMember(@Param("bizMemberId") Long bizMemberId);

    List<String> selectPlaceImages(@Param("placeId") Long placeId);

    int updatePlace(BusinessPlaceUpdateDto place);

    int deletePlaceImage(@Param("placeId") Long placeId, @Param("imageUrl") String imageUrl);
}
