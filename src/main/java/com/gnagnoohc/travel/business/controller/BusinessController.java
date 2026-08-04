package com.gnagnoohc.travel.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.business.dto.BusinessDashboardViewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceFormDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessSidebarContextDto;
import com.gnagnoohc.travel.business.exception.BusinessAuthException;
import com.gnagnoohc.travel.business.exception.NoPlaceRegisteredException;
import com.gnagnoohc.travel.business.sentiment.KeywordCount;
import com.gnagnoohc.travel.business.service.BusinessDashboardService;
import com.gnagnoohc.travel.business.service.BusinessExtraInfoCatalog;
import com.gnagnoohc.travel.business.service.BusinessPlaceService;
import com.gnagnoohc.travel.business.service.BusinessReservationService;
import com.gnagnoohc.travel.business.service.BusinessReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BusinessController {

    private static final String DEFAULT_FILTER_LABEL = "전체";

    private final BusinessDashboardService businessDashboardService;
    private final BusinessReservationService businessReservationService;
    private final BusinessPlaceService businessPlaceService;
    private final BusinessReviewService businessReviewService;
    private final ObjectMapper objectMapper;

    @GetMapping("/business/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        BusinessDashboardViewDto view = businessDashboardService.getDashboard(memberId);

        addSidebarAttributes(model, view.getSidebar());
        model.addAttribute("todayLabel", view.getTodayLabel());
        model.addAttribute("todayReservations", view.getTodayReservations());
        model.addAttribute("monthlyTrend", view.getMonthlyTrend());
        model.addAttribute("monthlyCount", view.getMonthlyCount());
        model.addAttribute("todayVisitors", view.getTodayVisitors());
        model.addAttribute("reviewSentiment", view.getReviewSentiment());
        model.addAttribute("keywordCloudJson", toKeywordCloudJson(view.getReviewSentiment().getKeywords()));

        return "business/dashboard";
    }

    //예약 관리
    @GetMapping("/business/reservations")
    public String reservations(
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model
    ) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);

        addSidebarAttributes(model, ctx);
        addFilterAttributes(model, status,
                ReservationFilter.toTabs(businessReservationService.getStatusCounts(ctx.getPlaceId(), memberId)));
        model.addAttribute("todayLabel", businessDashboardService.todayLabel());
        model.addAttribute("reservations", businessReservationService.getReservations(
                ctx.getPlaceId(), memberId, ReservationFilter.toStatusName(status)));

        return "business/reservations";
    }

    //후기 확인 : 답글은 커뮤니티 상세(댓글)에서 처리하므로 여기서는 목록만 보여준다. 감성분석 결과(긍정/중립/부정)로 필터링 가능
    @GetMapping("/business/reviews")
    public String reviews(
            @RequestParam(required = false) String sentiment,
            HttpSession session,
            Model model
    ) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);

        addSidebarAttributes(model, ctx);
        addFilterAttributes(model, sentiment,
                ReviewFilter.toTabs(businessReviewService.getSentimentCounts(ctx.getPlaceId())));
        model.addAttribute("reviews", businessReviewService.getReviews(
                ctx.getPlaceId(), ReviewFilter.toSentimentValue(sentiment)));

        return "business/reviews";
    }

    //마감 관리 : 즉시 예약 마감 토글 화면
    @GetMapping("/business/closure")
    public String closure(HttpSession session, Model model) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        addSidebarAttributes(model, businessDashboardService.getSidebarContext(memberId));
        return "business/closure";
    }

    //업소관리
    @GetMapping("/business/venue")
    public String venue(
            @RequestParam(defaultValue = "false") boolean edit,
            HttpSession session,
            Model model
    ) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        BusinessPlaceOverviewDto overview = businessPlaceService.findOverview(memberId);
        model.addAttribute("place", overview);
        addExtraInfoOptions(model);

        // 아직 업소를 등록하지 않았으면 사이드바에 채울 값 자체가 없으므로 등록 화면 분기만 준비한다
        if (overview == null) {
            model.addAttribute("canRegister", businessPlaceService.isBusinessMember(memberId));
            return "business/venue";
        }

        addSidebarAttributes(model, businessDashboardService.getSidebarContext(memberId));
        // 읽기뷰도 React 참고 화면과 동일하게 카테고리/지역/주소/소개/전체 사진을 보여줘야 해서 항상 상세 조회
        model.addAttribute("placeDetail", businessPlaceService.findDetail(memberId));
        model.addAttribute("editing", edit);

        return "business/venue";
    }

    //업소 등록
    @PostMapping("/business/venue/register")
    public String registerVenue(
            @ModelAttribute BusinessPlaceFormDto form,
            @RequestParam(required = false) List<MultipartFile> images,
            HttpSession session
    ) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessPlaceService.registerPlace(memberId, form, images);
        return "redirect:/business/venue";
    }

    //업소 정보 수정
    @PostMapping("/business/venue/update")
    public String updateVenue(
            @ModelAttribute BusinessPlaceFormDto form,
            // 수정 화면에서 드래그로 정한 최종 사진 순서(기존/신규 카드 통합). 기존 사진은 URL, 신규 사진은 "new" 토큰.
            // "new" 토큰은 등장하는 순서대로 newImages의 파일과 하나씩 매칭된다.
            @RequestParam(required = false) List<String> photoOrder,
            @RequestParam(required = false) List<String> removeImageUrls,
            @RequestParam(required = false) List<MultipartFile> newImages,
            HttpSession session
    ) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        businessPlaceService.updatePlace(memberId, form, photoOrder, removeImageUrls, newImages);
        return "redirect:/business/venue";
    }

    /* ------------------- 예약 관리 액션 ------------------- */

    //예약관리 : 결제완료(PAID) → 예약확정(CONFIRMED) 승인 (상태 전환은 reservation 파트 ReservationService 호출)
    @PostMapping("/business/reservations/{reservationId}/confirm")
    public String confirmReservation(@PathVariable Long reservationId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        return runReservationAction(redirectAttributes,
                () -> businessReservationService.confirm(reservationId, memberId));
    }

    //예약관리 : 결제완료(PAID) 예약 거절 → 환불 (reason은 사업자가 입력한 거절 사유, 비어있으면 서비스에서 기본 문구로 대체)
    @PostMapping("/business/reservations/{reservationId}/reject")
    public String rejectReservation(@PathVariable Long reservationId,
                                     @RequestParam(required = false) String reason,
                                     HttpSession session, RedirectAttributes redirectAttributes) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        return runReservationAction(redirectAttributes,
                () -> businessReservationService.reject(reservationId, memberId, reason));
    }

    //예약관리 : 취소 요청 승인 (환불 실행은 reservation 파트 PaymentService 호출)
    @PostMapping("/business/reservations/{reservationId}/cancel-approve")
    public String approveCancelReservation(@PathVariable Long reservationId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        return runReservationAction(redirectAttributes,
                () -> businessReservationService.approveCancel(reservationId, memberId));
    }

    //예약관리 : 취소 요청 거절 (PAID 원복은 reservation 파트 ReservationService 호출)
    @PostMapping("/business/reservations/{reservationId}/cancel-reject")
    public String rejectCancelReservation(@PathVariable Long reservationId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long memberId = BusinessSessionSupport.requireBusinessMemberId(session);
        return runReservationAction(redirectAttributes,
                () -> businessReservationService.rejectCancel(reservationId, memberId));
    }

    /**
     * 예약 관리 액션(승인/거절)의 공통 처리. 실패해도 흰 오류 페이지 대신 목록으로 돌아가 안내 배너를 띄운다.
     * - 업무 규칙 위반(이미 처리된 예약 등)은 서비스가 던진 메시지를 그대로 보여준다.
     * - PG 취소 API 실패·DB 오류 등은 사용자에게 보여줄 문구가 아니므로 일반 문구로 바꾸고 로그에 원인을 남긴다.
     *   (이 catch가 없으면 RestClientException 계열이 그대로 500으로 노출된다)
     * 미로그인/권한부족(BusinessAuthException)은 호출부에서 이미 처리되므로 여기로 오지 않는다.
     */
    private String runReservationAction(RedirectAttributes redirectAttributes, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("reservationError", e.getMessage());
        } catch (Exception e) {
            log.error("예약 관리 처리 중 예기치 않은 오류가 발생했습니다.", e);
            redirectAttributes.addFlashAttribute("reservationError", "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return "redirect:/business/reservations";
    }

    /* ------------------- 화면 공통 모델 ------------------- */

    // 모든 business 화면이 공유하는 사이드바(common/sidebar.jsp) 표시값
    private void addSidebarAttributes(Model model, BusinessSidebarContextDto ctx) {
        model.addAttribute("placeId", ctx.getPlaceId());
        model.addAttribute("bizName", ctx.getPlaceName());
        model.addAttribute("ownerName", ctx.getOwnerName());
        model.addAttribute("isClosed", ctx.getClosed());
        model.addAttribute("bizFirstImage", ctx.getFirstImage());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
    }

    /**
     * 업소 등록/수정 폼(venueFormFields.jsp)의 부가정보 항목 목록.
     * 업종을 바꾸면 화면에서 바로 해당 목록으로 갈아끼워야 해서(business-venue.js) 세 업종을 모두 내려준다.
     * 반려동물 동반 정보는 업종과 무관한 공통 섹션이라 따로 내려준다.
     */
    private void addExtraInfoOptions(Model model) {
        model.addAttribute("extraSectionsByType", BusinessExtraInfoCatalog.sectionsByPlaceType());
        model.addAttribute("petExtraOptions", BusinessExtraInfoCatalog.petOptions());
    }

    // 필터 탭(common/filterTabs.jsp) 표시값. tabs는 "라벨 -> 건수" 순서가 유지된 맵
    private void addFilterAttributes(Model model, String selectedLabel, Map<String, Integer> tabs) {
        model.addAttribute("filterTabs", tabs);
        model.addAttribute("filterSelected", selectedLabel == null ? DEFAULT_FILTER_LABEL : selectedLabel);
    }

    // wordcloud2.js가 기대하는 [[word, weight], ...] 형태로 직렬화. JSP에 <script>로 그대로 삽입되므로
    // Jackson으로 안전하게 이스케이프한다 (후기 키워드는 사용자가 작성한 텍스트에서 추출된 값이라 직접 문자열 결합하지 않음)
    private String toKeywordCloudJson(List<KeywordCount> keywords) {
        try {
            List<List<Object>> pairs = keywords.stream()
                    .map(k -> List.<Object>of(k.word(), k.count()))
                    .toList();
            return objectMapper.writeValueAsString(pairs);
        } catch (Exception e) {
            log.warn("워드클라우드 키워드 JSON 직렬화 실패", e);
            return "[]";
        }
    }

    // 아직 업소를 등록하지 않은 사업자가 dashboard/reservations에 접근하면 등록 화면으로 안내
    @ExceptionHandler(NoPlaceRegisteredException.class)
    public String handleNoPlaceRegistered() {
        return "redirect:/business/venue";
    }

    // 미로그인/권한부족 시 지정된 목적지로 리다이렉트 (미로그인 -> /auth/login, 권한부족 -> /)
    @ExceptionHandler(BusinessAuthException.class)
    public String handleBusinessAuth(BusinessAuthException e) {
        return "redirect:" + e.getRedirectTarget();
    }
}
