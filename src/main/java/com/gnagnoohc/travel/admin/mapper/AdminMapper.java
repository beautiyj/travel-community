package com.gnagnoohc.travel.admin.mapper;

import com.gnagnoohc.travel.admin.dto.AdminDashboardCountsDto;
import com.gnagnoohc.travel.admin.dto.AdminMonthlyTrendDto;
import com.gnagnoohc.travel.admin.dto.AdminPlaceOverviewDto;
import com.gnagnoohc.travel.admin.dto.AdminPlaceRegisterDto;
import com.gnagnoohc.travel.admin.dto.AdminRegionOptionDto;
import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    AdminPlaceOverviewDto selectPlaceOverviewByMember(@Param("bizMemberId") Long bizMemberId);

    List<AdminReservationDto> selectTodayReservations(@Param("placeId") Long placeId);

    AdminDashboardCountsDto selectDashboardCounts(@Param("placeId") Long placeId);

    List<AdminMonthlyTrendDto> selectMonthlyTrend(@Param("placeId") Long placeId);

    List<AdminReservationDto> selectReservationsByPlace(
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

    List<AdminRegionOptionDto> selectRegionOptions();

    Integer selectMemberType(@Param("memberId") Long memberId);

    int insertOwnerPlace(AdminPlaceRegisterDto place);

    int insertPlaceImage(
            @Param("placeId") Long placeId,
            @Param("imageUrl") String imageUrl,
            @Param("sortOrder") int sortOrder
    );
}
