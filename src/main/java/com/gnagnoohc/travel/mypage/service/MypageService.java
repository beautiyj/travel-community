package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import com.gnagnoohc.travel.mypage.dto.MypageDto;

public interface MypageService {

	MypageDto getMemberInfo(Long memberId);
	
	void updateMember(MypageDto member);

	void changePassword(MypageDto member);

	List<MypageDto> getReservationList(Long memberId);

	List<MypageDto> getWishlist(Long memberId);
}
