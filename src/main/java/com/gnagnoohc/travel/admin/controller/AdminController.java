package com.gnagnoohc.travel.admin.controller;

import com.gnagnoohc.travel.admin.dto.AdminDashboardViewDto;
import com.gnagnoohc.travel.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @RequestMapping("/admin/test")
    public String adminTest() {
        // /WEB-INF/views/admin/test.jsp
        return "admin/test";
    }

    // TODO: 로그인/세션 붙으면 memberId 파라미터 제거하고 인증 정보에서 가져오기
    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(defaultValue = "1") Long memberId, Model model) {
        AdminDashboardViewDto view = adminDashboardService.getDashboard(memberId);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", view.getPlaceName());
        model.addAttribute("ownerName", view.getOwnerName());
        model.addAttribute("isClosed", view.isClosed());
        model.addAttribute("todayLabel", view.getTodayLabel());
        model.addAttribute("todayReservations", view.getTodayReservations());
        model.addAttribute("monthlyTrend", view.getMonthlyTrend());
        model.addAttribute("monthlyCount", view.getMonthlyCount());
        model.addAttribute("pendingCount", view.getPendingCount());
        model.addAttribute("todayVisitors", view.getTodayVisitors());
        model.addAttribute("cancelRequestCount", view.getCancelRequestCount());

        return "admin/dashboard";
    }
}
