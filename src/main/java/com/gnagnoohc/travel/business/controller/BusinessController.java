package com.gnagnoohc.travel.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnagnoohc.travel.business.dto.BusinessDashboardViewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReviewDto;
import com.gnagnoohc.travel.business.dto.BusinessSidebarContextDto;
import com.gnagnoohc.travel.business.exception.NoPlaceRegisteredException;
import com.gnagnoohc.travel.business.sentiment.KeywordCount;
import com.gnagnoohc.travel.business.service.BusinessDashboardService;
import com.gnagnoohc.travel.business.service.BusinessPlaceService;
import com.gnagnoohc.travel.business.service.BusinessReservationService;
import com.gnagnoohc.travel.business.service.BusinessReviewService;
import com.gnagnoohc.travel.reservation.entity.ReservationStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequiredArgsConstructor
public class BusinessController {
    // 예약은 결제 즉시 확정되고 관리 대상은 취소요청 뿐이다
    private static final List<String> RESERVATION_STATUS_OPTIONS = List.of("전체", "취소요청", "확정", "완료", "취소");
    private static final List<String> REVIEW_SENTIMENT_OPTIONS = List.of("전체", "긍정", "중립", "부정");

    private final BusinessDashboardService businessDashboardService;
    private final BusinessReservationService businessReservationService;
    private final BusinessPlaceService businessPlaceService;
    private final BusinessReviewService businessReviewService;
    private final ObjectMapper objectMapper;

    // TODO: 로그인/세션 붙으면 memberId 파라미터 제거하고 인증 정보에서 가져오기
    @GetMapping("/business/dashboard")
    public String dashboard(@RequestParam(defaultValue = "1") Long memberId, Model model) {
        BusinessDashboardViewDto view = businessDashboardService.getDashboard(memberId);

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
        model.addAttribute("reviewSentiment", view.getReviewSentiment());
        model.addAttribute("keywordCloudJson", toKeywordCloudJson(view.getReviewSentiment().getKeywords()));

        return "business/dashboard";
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

    //예약 관리
    @GetMapping("/business/reservations")
    public String reservations(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(required = false) String status,
            Model model
    ) {
        BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);
        String statusParam = toReservationStatusName(status);
        List<BusinessReservationDto> reservations = businessReservationService.getReservations(ctx.getPlaceId(), memberId, statusParam);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", ctx.getPlaceName());
        model.addAttribute("ownerName", ctx.getOwnerName());
        model.addAttribute("isClosed", ctx.isClosed());
        model.addAttribute("bizFirstImage", ctx.getFirstImage());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
        model.addAttribute("statusOptions", RESERVATION_STATUS_OPTIONS);
        model.addAttribute("statusFilter", status == null ? "전체" : status);
        model.addAttribute("statusCounts", businessReservationService.getStatusCounts(ctx.getPlaceId(), memberId));
        model.addAttribute("todayLabel", businessDashboardService.todayLabel());
        model.addAttribute("reservations", reservations);

        return "business/reservations";
    }

    //예약관리 : 취소 요청 승인 (환불 실행은 reservation 파트 PaymentService 호출)
    @PostMapping("/business/reservations/{reservationId}/cancel-approve")
    public String approveCancelReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        businessReservationService.approveCancel(reservationId, memberId);
        return "redirect:/business/reservations?memberId=" + memberId;
    }

    //예약관리 : 취소 요청 거절 (PAID 원복은 reservation 파트 ReservationService 호출)
    @PostMapping("/business/reservations/{reservationId}/cancel-reject")
    public String rejectCancelReservation(@PathVariable Long reservationId, @RequestParam Long memberId) {
        businessReservationService.rejectCancel(reservationId, memberId);
        return "redirect:/business/reservations?memberId=" + memberId;
    }

    //필터
    private static String toReservationStatusName(String label) {
        if (label == null || "전체".equals(label)) return null;
        return switch (label) {
            case "취소요청" -> ReservationStatus.CANCEL_REQUESTED.name();
            case "확정" -> ReservationStatus.PAID.name();
            case "완료" -> ReservationStatus.COMPLETED.name();
            case "취소" -> ReservationStatus.CANCELED.name();
            default -> label;
        };
    }

    //마감 관리 : 즉시 예약 마감 토글 화면
    @GetMapping("/business/closure")
    public String closure(@RequestParam(defaultValue = "1") Long memberId, Model model) {
        BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", ctx.getPlaceName());
        model.addAttribute("ownerName", ctx.getOwnerName());
        model.addAttribute("isClosed", ctx.isClosed());
        model.addAttribute("bizFirstImage", ctx.getFirstImage());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
        model.addAttribute("placeId", ctx.getPlaceId());

        return "business/closure";
    }

    //업소관리
    @GetMapping("/business/venue")
    public String venue(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(defaultValue = "false") boolean edit,
            Model model
    ) {
        BusinessPlaceOverviewDto overview = businessPlaceService.findOverview(memberId);
        model.addAttribute("memberId", memberId);
        model.addAttribute("place", overview);

        if (overview != null) {
            BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);
            model.addAttribute("bizName", ctx.getPlaceName());
            model.addAttribute("ownerName", ctx.getOwnerName());
            model.addAttribute("isClosed", ctx.isClosed());
            model.addAttribute("bizFirstImage", ctx.getFirstImage());
            model.addAttribute("pendingCount", ctx.getPendingCount());
            model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());

            // 읽기뷰도 React 참고 화면과 동일하게 카테고리/지역/주소/소개/전체 사진을 보여줘야 해서 항상 상세 조회
            model.addAttribute("placeDetail", businessPlaceService.findDetail(memberId));

            if (edit) {
                model.addAttribute("editing", true);
//                model.addAttribute("regionOptions", businessPlaceService.getRegionOptions());
            }
        } else {
            boolean canRegister = businessPlaceService.isBusinessMember(memberId);
            model.addAttribute("canRegister", canRegister);
//            if (canRegister) {
//                model.addAttribute("regionOptions", businessPlaceService.getRegionOptions());
//            }
        }

        return "business/venue";
    }

    //업소 등록
    @PostMapping("/business/venue/register")
    public String registerVenue(
            @RequestParam Long memberId,
            @RequestParam String name,
            @RequestParam Integer placeType,
            @RequestParam(required = false) Long regionId,
            @RequestParam String address,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        businessPlaceService.registerPlace(memberId, name, placeType, regionId, address, description, images);
        return "redirect:/business/venue?memberId=" + memberId;
    }

    //업소 정보 수정
    @PostMapping("/business/venue/update")
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
        businessPlaceService.updatePlace(memberId, name, placeType, regionId, address, description, photoOrder, removeImageUrls, newImages);
        return "redirect:/business/venue?memberId=" + memberId;
    }

    //후기 확인 : 답글은 커뮤니티 상세(댓글)에서 처리하므로 여기서는 목록만 보여준다. 감성분석 결과(긍정/중립/부정)로 필터링 가능
    @GetMapping("/business/reviews")
    public String reviews(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(required = false) String sentiment,
            Model model
    ) {
        BusinessSidebarContextDto ctx = businessDashboardService.getSidebarContext(memberId);
        Integer sentimentParam = toSentimentValue(sentiment);
        List<BusinessReviewDto> reviews = businessReviewService.getReviews(ctx.getPlaceId(), sentimentParam);

        model.addAttribute("memberId", memberId);
        model.addAttribute("bizName", ctx.getPlaceName());
        model.addAttribute("ownerName", ctx.getOwnerName());
        model.addAttribute("isClosed", ctx.isClosed());
        model.addAttribute("bizFirstImage", ctx.getFirstImage());
        model.addAttribute("pendingCount", ctx.getPendingCount());
        model.addAttribute("cancelRequestCount", ctx.getCancelRequestCount());
        model.addAttribute("sentimentOptions", REVIEW_SENTIMENT_OPTIONS);
        model.addAttribute("sentimentFilter", sentiment == null ? "전체" : sentiment);
        model.addAttribute("sentimentCounts", businessReviewService.getSentimentCounts(ctx.getPlaceId()));
        model.addAttribute("reviews", reviews);

        return "business/reviews";
    }

    // 필터 탭의 한글 라벨 -> REVIEW_ANALYSIS.sentiment 값 (긍정=1, 중립=0, 부정=-1, 전체/미지정=null)
    private Integer toSentimentValue(String sentimentLabel) {
        if (sentimentLabel == null) {
            return null;
        }
        return switch (sentimentLabel) {
            case "긍정" -> 1;
            case "중립" -> 0;
            case "부정" -> -1;
            default -> null;
        };
    }

    // 아직 업소를 등록하지 않은 사업자가 dashboard/reservations에 접근하면 등록 화면으로 안내
    @ExceptionHandler(NoPlaceRegisteredException.class)
    public String handleNoPlaceRegistered(HttpServletRequest request) {
        String memberId = request.getParameter("memberId");
        return "redirect:/business/venue?memberId=" + (memberId != null ? memberId : "1");
    }
}
