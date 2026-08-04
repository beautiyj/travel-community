package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.repository.MypageRepository;

@Service
public class MypageServiceImpl implements MypageService {

    @Autowired
    private MypageRepository mypageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        String gender = member.getGender();
        if (gender == null || gender.isBlank()) {
            member.setGender(null);
        } else if (!"MALE".equals(gender) && !"FEMALE".equals(gender)) {
            throw new IllegalArgumentException("성별 선택값이 올바르지 않습니다.");
        }

        String address = member.getAddress();
        if (address == null || address.isBlank()) {
            member.setAddress(null);
        } else {
            address = address.trim();
            if (address.length() > 255) {
                throw new IllegalArgumentException(
                        "주소는 255자 이내로 입력해 주세요.");
            }
            member.setAddress(address);
        }

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
    
    @Override
    public void verifyCurrentPassword(
            Long memberId, String currentPassword) {
        requirePositive(memberId, "memberId");
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
        }

        String passwordHash = mypageRepository.getPasswordHash(memberId);
        if (passwordHash == null
                || !passwordEncoder.matches(currentPassword, passwordHash)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
    }

    @Override
    @Transactional
    public void changePassword(
            Long memberId,
            String currentPassword,
            String newPassword,
            String newPasswordCheck) {
        requirePositive(memberId, "memberId");
        verifyCurrentPassword(memberId, currentPassword);
        if (newPassword == null
                || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$")) {
            throw new IllegalArgumentException(
                    "새 비밀번호는 영문과 숫자를 포함한 8~20자여야 합니다.");
        }
        if (!newPassword.equals(newPasswordCheck)) {
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        String passwordHash = mypageRepository.getPasswordHash(memberId);
        if (passwordEncoder.matches(newPassword, passwordHash)) {
            throw new IllegalArgumentException(
                    "새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");
        }

        int updated = mypageRepository.updatePassword(
                memberId, passwordEncoder.encode(newPassword));
        if (updated != 1) {
            throw new IllegalStateException("비밀번호를 변경하지 못했습니다.");
        }
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
