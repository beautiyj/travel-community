package com.gnagnoohc.travel.mypage.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.dto.BusinessPlaceDto;
import com.gnagnoohc.travel.mypage.repository.BusinessRepository;

@Service
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;

    public BusinessServiceImpl(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Override
    public BusinessApplicationDto getApplication(Long memberId) {
        return businessRepository.findApplicationByMemberId(memberId);
    }

    @Override
    @Transactional
    public void resubmit(Long memberId, BusinessApplicationDto application) {
        validateMemberId(memberId);
        BusinessApplicationDto current =
                businessRepository.findApplicationByMemberId(memberId);
        if (current == null || !"APPROVED".equals(current.getStatus())) {
            throw new IllegalStateException(
                    "승인 완료된 사업자만 재승인을 요청할 수 있습니다.");
        }
        prepareApplication(memberId, application);
        if (businessRepository.resubmitApplication(application) != 1) {
            throw new IllegalStateException("사업자 재신청을 저장하지 못했습니다.");
        }
    }

    @Override
    public List<BusinessPlaceDto> getPlaces(Long memberId) {
        validateMemberId(memberId);
        return businessRepository.findPlacesByMemberId(memberId);
    }

    @Override
    @Transactional
    public void addPlaceImage(
            Long memberId, Long placeId, String imageUrl) {
        validateMemberId(memberId);
        if (placeId == null || placeId <= 0
                || businessRepository.countOwnedPlace(
                        placeId, memberId) != 1) {
            throw new IllegalArgumentException(
                    "본인 소유의 사업장만 이미지를 등록할 수 있습니다.");
        }
        if (imageUrl == null || imageUrl.isBlank()
                || businessRepository.insertPlaceImage(
                        placeId, imageUrl) != 1) {
            throw new IllegalStateException(
                    "사업장 이미지를 저장하지 못했습니다.");
        }
    }

    private void prepareApplication(
            Long memberId, BusinessApplicationDto application) {
        application.setMemberId(memberId);
        if (application.getDocumentUrl() == null
                || application.getDocumentUrl().isBlank()) {
            throw new IllegalArgumentException(
                    "사업자등록증 파일을 첨부해 주세요.");
        }
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
    }
}
