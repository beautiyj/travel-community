package com.gnagnoohc.travel.admin.controller;

import com.gnagnoohc.travel.admin.dto.AdminDashboardViewDto;
import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import com.gnagnoohc.travel.admin.dto.AdminSidebarContextDto;
import com.gnagnoohc.travel.admin.service.AdminDashboardService;
import com.gnagnoohc.travel.admin.service.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {
    // TODO: 예약상태 값 논의필요. 논의 후 리스트 값 변경 예정
    private static final List<String> RESERVATION_STATUS_OPTIONS = List.of("전체", "대기중", "확정", "완료", "취소");

    private final AdminDashboardService adminDashboardService;
    private final AdminReservationService adminReservationService;

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

    @GetMapping("/admin/reservations")
    public String reservations(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(required = false) String status,
            Model model
    ) {
        AdminSidebarContextDto ctx = adminDashboardService.getSidebarContext(memberId);
        String statusParam = (status == null || "전체".equals(status)) ? null : status;
        List<AdminReservationDto> reservations = adminReservationService.getReservations(ctx.getPlaceId(), memberId, statusParam);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", ctx.getPlaceName());
        model.addAttribute("ownerName", ctx.getOwnerName());
        model.addAttribute("isClosed", ctx.isClosed());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
        model.addAttribute("statusOptions", RESERVATION_STATUS_OPTIONS);
        model.addAttribute("statusFilter", status == null ? "전체" : status);
        model.addAttribute("reservations", reservations);

        return "admin/reservations";
    }

    @PostMapping("/admin/reservations/{reservationId}/accept")
    public String acceptReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.accept(reservationId, memberId);
        return "redirect:/admin/reservations?memberId=" + memberId;
    }

    @PostMapping("/admin/reservations/{reservationId}/reject")
    public String rejectReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.reject(reservationId, memberId);
        return "redirect:/admin/reservations?memberId=" + memberId;
    }
}
