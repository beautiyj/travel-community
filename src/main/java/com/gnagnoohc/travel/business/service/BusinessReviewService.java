package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessReviewDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessReviewService {

    private final BusinessMapper businessMapper;

    public List<BusinessReviewDto> getReviews(Long placeId) {
        return businessMapper.selectReviewsByPlace(placeId);
    }
}
