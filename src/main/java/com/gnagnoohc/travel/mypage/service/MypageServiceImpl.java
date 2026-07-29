package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public MypageDto getMemberByLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }
        return mypageRepository.getMemberByLoginId(loginId.trim());
    }
    
    @Override
    public int updateMember(MypageDto member) {
    	return mypageRepository.updateMember(member);
    }

    @Override
    public int updateProfileImage(
            Long memberId, String profileImgUrl) {
        requirePositive(memberId, "memberId");
        if (profileImgUrl == null || profileImgUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "프로필 이미지 경로가 올바르지 않습니다.");
        }
        return mypageRepository.updateProfileImage(
                memberId, profileImgUrl);
    }
    
//    @Override
//    public int changePassword(MypageDto member) {
//    	return mypageRepository.changePassword(member);
//    }
    
    @Override
    public List<MypageDto> getReservationList(Long memberId){
    	return mypageRepository.getReservationList(memberId);
    }

    @Override
    public List<MypageDto> getWishlist(Long memberId){
    	return mypageRepository.getWishlist(memberId);
    }
    
    @Override
    public int withdrawMember(Long memberId) {
    	return mypageRepository.withdrawMember(memberId);
    }
    
    @Override
    public int deleteWishlist(Long wishlistId, Long memberId) {
        requirePositive(wishlistId, "wishlistId");
        requirePositive(memberId, "memberId");
    	return mypageRepository.deleteWishlist(wishlistId, memberId);
    }

    @Override
    public boolean isWishlisted(Long memberId, Long placeId) {
        requirePositive(memberId, "memberId");
        requirePositive(placeId, "placeId");
        return mypageRepository.countWishlist(memberId, placeId) > 0;
    }

    @Override
    @Transactional
    public boolean toggleWishlist(Long memberId, Long placeId) {
        requirePositive(memberId, "memberId");
        requirePositive(placeId, "placeId");

        if (mypageRepository.countWishlist(memberId, placeId) > 0) {
            mypageRepository.deleteWishlistByPlace(memberId, placeId);
            return false;
        }

        mypageRepository.addWishlist(memberId, placeId);
        return true;
    }

    @Override
    public int cancelReservation(Long reservationId) {
    	return mypageRepository.cancelReservation(reservationId);
    }
    
    @Override
    public List<MypageDto> getReservationCompleteList(Long memberId) {
        return mypageRepository.getReservationCompleteList(memberId);
    }

    @Override
    public List<MypageDto> getPaymentCompleteList(Long memberId) {
        return mypageRepository.getPaymentCompleteList(memberId);
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number");
        }
    }
}
