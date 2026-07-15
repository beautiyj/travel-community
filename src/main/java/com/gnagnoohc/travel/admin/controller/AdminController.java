package com.gnagnoohc.travel.admin.controller;

import com.gnagnoohc.travel.admin.dto.AdminDashboardViewDto;
import com.gnagnoohc.travel.admin.dto.AdminPlaceOverviewDto;
import com.gnagnoohc.travel.admin.dto.AdminReservationDto;
import com.gnagnoohc.travel.admin.dto.AdminSidebarContextDto;
import com.gnagnoohc.travel.admin.exception.NoPlaceRegisteredException;
import com.gnagnoohc.travel.admin.service.AdminDashboardService;
import com.gnagnoohc.travel.admin.service.AdminPlaceService;
import com.gnagnoohc.travel.admin.service.AdminReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {
    // TODO: 예약상태 값 논의필요. 논의 후 리스트 값 변경 예정
    private static final List<String> RESERVATION_STATUS_OPTIONS = List.of("전체", "대기중", "확정", "완료", "취소");

    private final AdminDashboardService adminDashboardService;
    private final AdminReservationService adminReservationService;
    private final AdminPlaceService adminPlaceService;

    // TODO: 로그인/세션 붙으면 memberId 파라미터 제거하고 인증 정보에서 가져오기
    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(defaultValue = "1") Long memberId, Model model) {
        AdminDashboardViewDto view = adminDashboardService.getDashboard(memberId);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", view.getPlaceName());
        model.addAttribute("ownerName", view.getOwnerName());
        model.addAttribute("isClosed", view.isClosed());
        model.addAttribute("bizFirstImage", view.getFirstImage());
        model.addAttribute("todayLabel", view.getTodayLabel());
        model.addAttribute("todayReservations", view.getTodayReservations());
        model.addAttribute("monthlyTrend", view.getMonthlyTrend());
        model.addAttribute("monthlyCount", view.getMonthlyCount());
        model.addAttribute("pendingCount", view.getPendingCount());
        model.addAttribute("todayVisitors", view.getTodayVisitors());
        model.addAttribute("cancelRequestCount", view.getCancelRequestCount());

        return "admin/dashboard";
    }

    //예약 관리
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
        model.addAttribute("bizFirstImage", ctx.getFirstImage());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
        model.addAttribute("statusOptions", RESERVATION_STATUS_OPTIONS);
        model.addAttribute("statusFilter", status == null ? "전체" : status);
        model.addAttribute("reservations", reservations);

        return "admin/reservations";
    }

    //예약관리 : 수락
    @PostMapping("/admin/reservations/{reservationId}/accept")
    public String acceptReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.accept(reservationId, memberId);
        return "redirect:/admin/reservations?memberId=" + memberId;
    }

    //예약관리 : 거절
    @PostMapping("/admin/reservations/{reservationId}/reject")
    public String rejectReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        adminReservationService.reject(reservationId, memberId);
        return "redirect:/admin/reservations?memberId=" + memberId;
    }

    @GetMapping("/admin/venue")
    public String venue(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(defaultValue = "false") boolean edit,
            Model model
    ) {
        AdminPlaceOverviewDto overview = adminPlaceService.findOverview(memberId);
        model.addAttribute("memberId", memberId);
        model.addAttribute("place", overview);

        if (overview != null) {
            AdminSidebarContextDto ctx = adminDashboardService.getSidebarContext(memberId);
            model.addAttribute("bizName", ctx.getPlaceName());
            model.addAttribute("ownerName", ctx.getOwnerName());
            model.addAttribute("isClosed", ctx.isClosed());
            model.addAttribute("bizFirstImage", ctx.getFirstImage());
            model.addAttribute("pendingCount", ctx.getPendingCount());
            model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());

            // 읽기뷰도 React 참고 화면과 동일하게 카테고리/지역/주소/소개/전체 사진을 보여줘야 해서 항상 상세 조회
            model.addAttribute("placeDetail", adminPlaceService.findDetail(memberId));

            if (edit) {
                model.addAttribute("editing", true);
//                model.addAttribute("regionOptions", adminPlaceService.getRegionOptions());
            }
        } else {
            boolean canRegister = adminPlaceService.isBusinessMember(memberId);
            model.addAttribute("canRegister", canRegister);
//            if (canRegister) {
//                model.addAttribute("regionOptions", adminPlaceService.getRegionOptions());
//            }
        }

        return "admin/venue";
    }

    @PostMapping("/admin/venue/register")
    public String registerVenue(
            @RequestParam Long memberId,
            @RequestParam String name,
            @RequestParam Integer placeType,
            @RequestParam(required = false) Long regionId,
            @RequestParam String address,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        adminPlaceService.registerPlace(memberId, name, placeType, regionId, address, description, images);
        return "redirect:/admin/venue?memberId=" + memberId;
    }

    @PostMapping("/admin/venue/update")
    public String updateVenue(
            @RequestParam Long memberId,
            @RequestParam String name,
            @RequestParam Integer placeType,
            @RequestParam(required = false) Long regionId,
            @RequestParam String address,
            @RequestParam(required = false) String description,
            // 수정 화면에서 드래그로 정한 최종 사진 순서(기존/신규 카드 통합). 기존 사진은 URL, 신규 사진은 "new" 토큰.
            // "new" 토큰은 등장하는 순서대로 newImages의 파일과 하나씩 매칭된다.
            @RequestParam(required = false) List<String> photoOrder,
            @RequestParam(required = false) List<String> removeImageUrls,
            @RequestParam(required = false) List<MultipartFile> newImages
    ) {
        adminPlaceService.updatePlace(memberId, name, placeType, regionId, address, description, photoOrder, removeImageUrls, newImages);
        return "redirect:/admin/venue?memberId=" + memberId;
    }

    // 아직 업소를 등록하지 않은 사업자가 dashboard/reservations에 접근하면 등록 화면으로 안내
    @ExceptionHandler(NoPlaceRegisteredException.class)
    public String handleNoPlaceRegistered(HttpServletRequest request) {
        String memberId = request.getParameter("memberId");
        return "redirect:/admin/venue?memberId=" + (memberId != null ? memberId : "1");
    }
}
