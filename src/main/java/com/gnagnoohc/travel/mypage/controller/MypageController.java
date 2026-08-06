package com.gnagnoohc.travel.mypage.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.service.BusinessMediaStorage;
import com.gnagnoohc.travel.mypage.service.MypageService;
import com.gnagnoohc.travel.storage.ImageStorage;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/mypage")
public class MypageController {

    @Autowired
    private MypageService mypageService;

    @Autowired
    private BusinessMediaStorage mediaStorage;

    @Autowired
    private ImageStorage imageStorage;

    @ModelAttribute("businessAccount")
    public boolean businessAccount() {
        return false;
    }

    @GetMapping("")
    public String mypage(HttpSession session) {
        Long memberId = getMemberId(session);
        if (memberId == null) {
            return "redirect:/auth/login";
        }

        MypageDto member = mypageService.getMemberInfo(memberId);
        if (member == null) {
            session.invalidate();
            return "redirect:/auth/login";
        }

        if (Integer.valueOf(2).equals(member.getMemberType())) {
            return "redirect:/mypage/business-info";
        }
        return "redirect:/mypage/info";
    }

    @GetMapping("/info")
    public String memberInfo(Model model, HttpSession session) {

        MypageDto member =
                mypageService.getMemberInfo(getMemberId(session));

        model.addAttribute("member", member);

        return "mypage/common/memberInfo";
    }

    @GetMapping("/edit")
    public String editForm(Model model, HttpSession session) {
        Long memberId = getMemberId(session);

        MypageDto member =
                mypageService.getMemberInfo(memberId);

        model.addAttribute("member", member);

        return "mypage/common/memberEdit";
    }

    @PostMapping("/edit")
    public String editMember(
            MypageDto member,
            @RequestParam(value = "profileImage", required = false)
            MultipartFile profileImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long memberId = getMemberId(session);
        member.setMemberId(memberId);

        try {
            mypageService.updateMember(member);

            if (profileImage != null && !profileImage.isEmpty()) {
                String previousImageUrl =
                        mypageService.getMemberInfo(memberId).getProfileImgUrl();
                String imageUrl =
                        mediaStorage.storeProfile(profileImage, memberId);
                mypageService.updateProfileImage(memberId, imageUrl);
                imageStorage.delete(previousImageUrl);
            }

            redirectAttributes.addFlashAttribute(
                    "message", "회원정보를 수정했습니다.");
            return "redirect:/mypage/info";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/edit";
        }
    }

    @GetMapping("/password")
    public String passwordForm(Model model, HttpSession session) {
        model.addAttribute("member",
                mypageService.getMemberInfo(getMemberId(session)));
        return "mypage/common/password";
    }

    @PostMapping("/password")
    public String changePassword(
            MypageDto member,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            mypageService.changePassword(
                    getMemberId(session),
                    member.getCurrentPassword(),
                    member.getNewPassword(),
                    member.getNewPasswordCheck());
            redirectAttributes.addFlashAttribute(
                    "message", "비밀번호를 변경했습니다.");
            return "redirect:/mypage/info";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/password";
        }
    }

    @GetMapping("/reservation")
    public String reservation(
            @RequestParam(name = "status", required = false) String status,
            Model model,
            HttpSession session) {

        Long memberId = getMemberId(session);
        if (memberId == null) {
            return "redirect:/auth/login";
        }

        List<MypageDto> allReservations = mypageService.getReservationList(memberId);
        String statusFilter = normalizeReservationStatus(status);
        List<MypageDto> reservationList = allReservations.stream()
                .filter(reservation -> statusFilter == null
                        || matchesReservationStatus(reservation.getStatus(), statusFilter))
                .toList();

        model.addAttribute("reservationTabs", buildReservationTabs(allReservations));
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("reservationTotalCount", reservationList.size());
        model.addAttribute("todayLabel", LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)));
        model.addAttribute("reservationList", reservationList);
        model.addAttribute("member", mypageService.getMemberInfo(memberId));

        return "mypage/user/reservation";
    }

    private List<Map<String, Object>> buildReservationTabs(List<MypageDto> reservations) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("전체", null);
        filters.put("예약대기", "PENDING");
        filters.put("결제완료", "PAID");
        filters.put("취소요청", "CANCEL_REQUESTED");
        filters.put("예약 확정", "CONFIRMED");
        filters.put("이용 완료", "COMPLETED");
        filters.put("취소", "CANCELED");

        List<Map<String, Object>> tabs = new ArrayList<>();
        filters.forEach((label, value) -> {
            long count = value == null
                    ? reservations.size()
                    : reservations.stream()
                            .filter(reservation -> matchesReservationStatus(reservation.getStatus(), value))
                            .count();
            Map<String, Object> tab = new LinkedHashMap<>();
            tab.put("label", label);
            tab.put("value", value);
            tab.put("count", count);
            tabs.add(tab);
        });
        return tabs;
    }

    private String normalizeReservationStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case "PENDING", "PAID", "CANCEL_REQUESTED", "CONFIRMED", "COMPLETED", "CANCELED" -> status;
            default -> null;
        };
    }

    private boolean matchesReservationStatus(String actualStatus, String expectedStatus) {
        if ("CANCELED".equals(expectedStatus)) {
            return "CANCELED".equals(actualStatus) || "CANCELLED".equals(actualStatus);
        }
        return expectedStatus.equals(actualStatus);
    }

    @GetMapping("/wishlist")
    public String wishlist(Model model, HttpSession session) {

        Long memberId = getMemberId(session);
        if (memberId == null) {
            return "redirect:/auth/login";
        }

        List<MypageDto> wishlist = mypageService.getWishlist(memberId);

        model.addAttribute("wishlist", wishlist);
        model.addAttribute("member", mypageService.getMemberInfo(memberId));

        return "mypage/user/wishlist";
    }

    @GetMapping("/withdraw")
    public String withdrawForm(Model model, HttpSession session) {
        model.addAttribute("member",
                mypageService.getMemberInfo(getMemberId(session)));
        return "mypage/common/withdraw";
    }

    @PostMapping("/withdraw")
    public String withdrawMember(HttpSession session) {

        int result = mypageService.withdrawMember(getMemberId(session));

        session.invalidate();

        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/";
    }

    @PostMapping("/wishlist/delete")
    public String deleteWishlist(
            @RequestParam("wishlistId") Long wishlistId,
            HttpSession session) {

        Long memberId = getMemberId(session);
        if (memberId != null) {
            mypageService.deleteWishlist(wishlistId, memberId);
        }

        return "redirect:/mypage/wishlist";
    }

    @PostMapping("/wishlist/toggle")
    @ResponseBody
    public ResponseEntity<WishlistResponse> toggleWishlist(
            @RequestParam("placeId") Long placeId,
            HttpSession session) {

        Long memberId = getMemberId(session);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WishlistResponse(false, "로그인이 필요합니다."));
        }

        boolean wishlisted = mypageService.toggleWishlist(memberId, placeId);
        return ResponseEntity.ok(new WishlistResponse(
                wishlisted,
                wishlisted ? "찜목록에 추가했습니다." : "찜목록에서 삭제했습니다."));
    }

    @GetMapping("/wishlist/status/{placeId}")
    @ResponseBody
    public ResponseEntity<WishlistResponse> wishlistStatus(
            @PathVariable Long placeId,
            HttpSession session) {

        Long memberId = getMemberId(session);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WishlistResponse(false, "로그인이 필요합니다."));
        }

        return ResponseEntity.ok(new WishlistResponse(
                mypageService.isWishlisted(memberId, placeId), null));
    }

    private Long getMemberId(HttpSession session) {
        Object loginMember = session.getAttribute("loginMember");
        if (loginMember instanceof LoginMemberDto member) {
            return (long) member.getMemberId();
        }
        return null;
    }

    public record WishlistResponse(boolean wishlisted, String message) {
    }

    @PostMapping("/reservation/cancel")
    public String cancelReservation(
            @RequestParam("reservationId") Long reservationId) {

        int result = mypageService.cancelReservation(reservationId);

        return "redirect:/mypage/reservation";
    }

    @GetMapping("/payment")
    public String paymentList(Model model, HttpSession session) {

        Long memberId = getMemberId(session);
        List<MypageDto> paymentList =
                mypageService.getPaymentCompleteList(memberId);

        model.addAttribute("paymentList", paymentList);
        model.addAttribute("member", mypageService.getMemberInfo(memberId));

        return "mypage/user/payment";
    }
}
