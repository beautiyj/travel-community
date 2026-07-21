package com.gnagnoohc.travel.mypage.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.mypage.dto.MypageDto;

@Mapper
public interface MypageRepository {

    MypageDto getMemberInfo(Long memberId);

    void updateMember(MypageDto member);

    void changePassword(MypageDto member);

    List<MypageDto> getReservationList(Long memberId);
    
    List<MypageDto> getWishlist(Long memberId);
    
    void withdrawMember(Long memberId);
}