package com.gnagnoohc.travel.admin.mapper;

import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

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
}
