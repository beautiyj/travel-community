package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessDashboardCountsDto;
import com.gnagnoohc.travel.business.dto.BusinessDashboardViewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessSidebarContextDto;
import com.gnagnoohc.travel.business.exception.NoPlaceRegisteredException;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BusinessDashboardService {

    private static final String[] KOREAN_DAY_OF_WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    private final BusinessMapper businessMapper;
    private final ReviewSentimentService reviewSentimentService;

    // 사이드바(업소명/대표명/마감상태/뱃지 카운트)에만 필요한 최소 데이터. 대시보드 외 다른 business 페이지에서도 재사용
    public BusinessSidebarContextDto getSidebarContext(Long bizMemberId) {
        BusinessPlaceOverviewDto overview = requireOverview(bizMemberId);
        return toSidebarContext(overview, businessMapper.selectDashboardCounts(overview.getPlaceId()));
    }

    public BusinessDashboardViewDto getDashboard(Long bizMemberId) {
        BusinessPlaceOverviewDto overview = requireOverview(bizMemberId);
        Long placeId = overview.getPlaceId();
        BusinessDashboardCountsDto counts = businessMapper.selectDashboardCounts(placeId);

        return BusinessDashboardViewDto.builder()
                .sidebar(toSidebarContext(overview, counts))
                .todayLabel(todayLabel())
                .todayReservations(businessMapper.selectTodayReservations(placeId))
                .monthlyTrend(businessMapper.selectMonthlyTrend(placeId))
                .monthlyCount(counts.getMonthlyCount())
                .todayVisitors(counts.getTodayVisitors())
                .reviewSentiment(reviewSentimentService.getSentimentSummary(placeId))
                .build();
    }

    private BusinessSidebarContextDto toSidebarContext(BusinessPlaceOverviewDto overview, BusinessDashboardCountsDto counts) {
        return BusinessSidebarContextDto.builder()
                .placeId(overview.getPlaceId())
                .placeName(overview.getPlaceName())
                .ownerName(overview.getOwnerName())
                .closed(overview.getClosed())
                .pendingCount(counts.getPendingCount())
                .cancelRequestCount(counts.getCancelRequestCount())
                .firstImage(overview.getFirstImage())
                .build();
    }

    private BusinessPlaceOverviewDto requireOverview(Long bizMemberId) {
        BusinessPlaceOverviewDto overview = businessMapper.selectPlaceOverviewByMember(bizMemberId);
        if (overview == null) {
            throw new NoPlaceRegisteredException("등록된 업소가 없습니다.");
        }
        return overview;
    }

    public String todayLabel() {
        LocalDate today = LocalDate.now();
        String dow = KOREAN_DAY_OF_WEEK[today.getDayOfWeek().getValue() - 1];
        return today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) + " (" + dow + ")";
    }
}
