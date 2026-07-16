package com.gnagnoohc.travel.reservation.mapper;

import com.gnagnoohc.travel.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {
    void insert(Reservation reservation);
    Reservation findById(Long reservationId);
    List<Reservation> findByMemberId(Long memberId);
    void updateStatus(@Param("reservationId") Long reservationId, @Param("status") String status);

    /** 슬롯 선점 체크: 같은 회원이 같은 장소·날짜에 이미 활성(예약중/예약완료) 예약이 있는지 */
    int countActive(@Param("memberId") Long memberId,
                    @Param("placeId") Long placeId,
                    @Param("visitDate") LocalDate visitDate);

    /** 스케줄러: cutoff 이전에 생성됐는데 아직 '예약중'인 건을 '예약만료'로 일괄 전환. 처리 건수 반환 */
    int expirePending(@Param("cutoff") LocalDateTime cutoff);
}
