package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessDashboardCountsDto;
import com.gnagnoohc.travel.business.dto.BusinessDashboardViewDto;
import com.gnagnoohc.travel.business.dto.BusinessMonthlyTrendDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessSidebarContextDto;
import com.gnagnoohc.travel.business.exception.NoPlaceRegisteredException;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessDashboardService {

    private static final String[] KOREAN_DAY_OF_WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    private final BusinessMapper businessMapper;
    private final ReviewSentimentService reviewSentimentService;

    // 사이드바(업소명/대표명/마감상태/뱃지 카운트)에만 필요한 최소 데이터. 대시보드 외 다른 business 페이지에서도 재사용
    public BusinessSidebarContextDto getSidebarContext(Long bizMemberId) {
        BusinessPlaceOverviewDto overview = requireOverview(bizMemberId);
        BusinessDashboardCountsDto counts = businessMapper.selectDashboardCounts(overview.getPlaceId());

        return BusinessSidebarContextDto.builder()
                .placeId(overview.getPlaceId())
                .placeName(overview.getPlaceName())
                .ownerName(overview.getOwnerName())
                .isClosed(overview.isClosed())
                .pendingCount(counts.getPendingCount())
                .cancelRequestCount(counts.getCancelRequestCount())
                .firstImage(overview.getFirstImage())
                .build();
    }

    public BusinessDashboardViewDto getDashboard(Long bizMemberId) {
        BusinessPlaceOverviewDto overview = requireOverview(bizMemberId);

        List<BusinessReservationDto> todayReservations = businessMapper.selectTodayReservations(overview.getPlaceId());
        BusinessDashboardCountsDto counts = businessMapper.selectDashboardCounts(overview.getPlaceId());
        List<BusinessMonthlyTrendDto> monthlyTrend = businessMapper.selectMonthlyTrend(overview.getPlaceId());

        return BusinessDashboardViewDto.builder()
                .placeName(overview.getPlaceName())
                .ownerName(overview.getOwnerName())
                .isClosed(overview.isClosed())
                .firstImage(overview.getFirstImage())
                .todayLabel(todayLabel())
                .todayReservations(todayReservations)
                .monthlyTrend(monthlyTrend)
                .monthlyCount(counts.getMonthlyCount())
                .pendingCount(counts.getPendingCount())
                .todayVisitors(counts.getTodayVisitors())
                .cancelRequestCount(counts.getCancelRequestCount())
                .reviewSentiment(reviewSentimentService.getSentimentSummary(overview.getPlaceId()))
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
