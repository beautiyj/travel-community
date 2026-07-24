package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.repository.MypageRepository;

@Service
public class MypageServiceImpl implements MypageService {

    @Autowired
    private MypageRepository mypageRepository;

    @Override
    public MypageDto getMemberInfo(Long memberId) {
        return mypageRepository.getMemberInfo(memberId);
    }
    
    @Override
    public void updateMember(MypageDto member) {
    	mypageRepository.updateMember(member);
    }
    
    @Override
    public void changePassword(MypageDto member) {
    	mypageRepository.changePassword(member);
    }
    
    @Override
    public List<MypageDto> getReservationList(Long memberId){
    	return mypageRepository.getReservationList(memberId);
    }

    @Override
    public List<MypageDto> getWishlist(Long memberId){
    	return mypageRepository.getWishlist(memberId);
    }
    
    @Override
    public void withdrawMember(Long memberId) {
    	mypageRepository.withdrawMember(memberId);
    }
    
    @Override
    public void deleteWishlist(Long wishlistId) {
        mypageRepository.deleteWishlist(wishlistId);
    }

    @Override
    public void cancelReservation(Long reservationId) {
        mypageRepository.cancelReservation(reservationId);
    }
    
    
}