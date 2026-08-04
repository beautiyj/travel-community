package com.gnagnoohc.travel.mypage.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.mypage.dto.MypageDto;

@Mapper
public interface MypageRepository {

    MypageDto getMemberInfo(Long memberId);

    MypageDto getMemberByLoginId(@Param("loginId") String loginId);

    int updateMember(MypageDto member);

    int updateProfileImage(@Param("memberId") Long memberId,
                           @Param("profileImgUrl") String profileImgUrl);

    String getPasswordHash(Long memberId);

    int updatePassword(@Param("memberId") Long memberId,
                       @Param("passwordHash") String passwordHash);

    List<MypageDto> getReservationList(Long memberId);
    
    List<MypageDto> getWishlist(Long memberId);
    
    int withdrawMember(Long memberId);
    
    int deleteWishlist(@Param("wishlistId") Long wishlistId,
                       @Param("memberId") Long memberId);

    int countWishlist(@Param("memberId") Long memberId,
                      @Param("placeId") Long placeId);

    int addWishlist(@Param("memberId") Long memberId,
                    @Param("placeId") Long placeId);

    int deleteWishlistByPlace(@Param("memberId") Long memberId,
                              @Param("placeId") Long placeId);

	int cancelReservation(Long reservationId);
    
	List<MypageDto> getReservationCompleteList(Long memberId);

	List<MypageDto> getPaymentCompleteList(Long memberId);
    
}
