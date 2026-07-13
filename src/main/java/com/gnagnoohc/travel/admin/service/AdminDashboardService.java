package com.gnagnoohc.travel.admin.service;

import com.gnagnoohc.travel.admin.dto.AdminDashboardCountsDto;
import com.gnagnoohc.travel.admin.dto.AdminDashboardViewDto;
import com.gnagnoohc.travel.admin.dto.AdminPlaceOverviewDto;
import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import com.gnagnoohc.travel.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final String[] KOREAN_DAY_OF_WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    private final AdminMapper adminMapper;

    public AdminDashboardViewDto getDashboard(Long bizMemberId) {
        AdminPlaceOverviewDto overview = adminMapper.selectPlaceOverviewByMember(bizMemberId);
        if (overview == null) {
            throw new IllegalArgumentException("등록된 업소가 없습니다.");
        }

        List<AdminReservationDto> todayReservations = adminMapper.selectTodayReservations(overview.getPlaceId());
        AdminDashboardCountsDto counts = adminMapper.selectDashboardCounts(overview.getPlaceId());

        return AdminDashboardViewDto.builder()
                .placeName(overview.getPlaceName())
                .ownerName(overview.getOwnerName())
                .isClosed(overview.isClosed())
                .todayLabel(todayLabel())
                .todayReservations(todayReservations)
                .monthlyCount(counts.getMonthlyCount())
                .pendingCount(counts.getPendingCount())
                .todayVisitors(counts.getTodayVisitors())
                .cancelRequestCount(counts.getCancelRequestCount())
                .build();
    }

    private String todayLabel() {
        LocalDate today = LocalDate.now();
        String dow = KOREAN_DAY_OF_WEEK[today.getDayOfWeek().getValue() - 1];
        return today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) + " (" + dow + ")";
    }
}
