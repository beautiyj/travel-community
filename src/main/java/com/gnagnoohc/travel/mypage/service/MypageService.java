package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import com.gnagnoohc.travel.mypage.dto.MypageDto;

public interface MypageService {

	MypageDto getMemberInfo(Long memberId);

	MypageDto getMemberByLoginId(String loginId);
	
	int updateMember(MypageDto member);

	int updateProfileImage(Long memberId, String profileImgUrl);

//	int changePassword(MypageDto member);

	List<MypageDto> getReservationList(Long memberId);

	List<MypageDto> getWishlist(Long memberId);
	
	int withdrawMember(Long memberId);
	
	int deleteWishlist(Long wishlistId, Long memberId);

	boolean isWishlisted(Long memberId, Long placeId);

	boolean toggleWishlist(Long memberId, Long placeId);

	int cancelReservation(Long reservationId);
	
	List<MypageDto> getReservationCompleteList(Long memberId);

	List<MypageDto> getPaymentCompleteList(Long memberId);
}
