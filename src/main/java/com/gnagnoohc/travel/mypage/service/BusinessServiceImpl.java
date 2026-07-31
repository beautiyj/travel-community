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
    public void submitApplication(
            Long memberId, BusinessApplicationDto application) {
        validateMemberId(memberId);
        BusinessApplicationDto current =
                businessRepository.findApplicationByMemberId(memberId);
        if (current != null && "PENDING".equals(current.getStatus())) {
            throw new IllegalStateException(
                    "승인 대기 중에는 사업자등록증을 다시 제출할 수 없습니다.");
        }
        prepareApplication(memberId, application);
        if (businessRepository.saveApplication(application) != 1) {
            throw new IllegalStateException(
                    "사업자 승인 신청을 저장하지 못했습니다.");
        }
    }

    @Override
    @Transactional
    public void cancelPendingApplication(Long memberId) {
        validateMemberId(memberId);
        if (businessRepository.deletePendingApplication(memberId) != 1) {
            throw new IllegalStateException(
                    "승인 대기 중인 사업자 신청만 취소할 수 있습니다.");
        }
    }

    @Override
    public List<BusinessPlaceDto> getPlaces(Long memberId) {
        validateMemberId(memberId);
        return businessRepository.findPlacesByMemberId(memberId);
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
