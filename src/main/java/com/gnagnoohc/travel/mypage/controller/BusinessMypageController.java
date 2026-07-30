package com.gnagnoohc.travel.mypage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.mypage.dto.BusinessApplicationDto;
import com.gnagnoohc.travel.mypage.service.BusinessDocumentStorage;
import com.gnagnoohc.travel.mypage.service.BusinessMediaStorage;
import com.gnagnoohc.travel.mypage.service.BusinessService;
import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.service.MypageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/mypage/business-info")
public class BusinessMypageController {

    private final BusinessService businessService;
    private final MypageService mypageService;
    private final BusinessDocumentStorage documentStorage;
    private final BusinessMediaStorage mediaStorage;

    public BusinessMypageController(
            BusinessService businessService,
            MypageService mypageService,
            BusinessDocumentStorage documentStorage,
            BusinessMediaStorage mediaStorage) {
        this.businessService = businessService;
        this.mypageService = mypageService;
        this.documentStorage = documentStorage;
        this.mediaStorage = mediaStorage;
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        model.addAttribute("application",
                businessService.getApplication(member.getMemberId()));
        return "mypage/business/info";
    }

    @GetMapping("/info")
    public String info() {
        return "redirect:/mypage/business-info";
    }

    @GetMapping("/approval")
    public String approval(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        model.addAttribute("application",
                businessService.getApplication(member.getMemberId()));
        return "mypage/business/dashboard";
    }

    @GetMapping("/places")
    public String places(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        model.addAttribute(
                "places", businessService.getPlaces(member.getMemberId()));
        return "mypage/business/places";
    }

    @GetMapping("/edit")
    public String editForm(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        return "mypage/business/edit";
    }

    @PostMapping("/edit")
    public String edit(
            @ModelAttribute MypageDto form,
            @RequestParam(value = "profileImage", required = false)
            MultipartFile profileImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            form.setMemberId(member.getMemberId());
            form.setNickname(member.getNickname());
            mypageService.updateMember(form);

            if (profileImage != null && !profileImage.isEmpty()) {
                String imageUrl = mediaStorage.storeProfile(
                        profileImage, member.getMemberId());
                mypageService.updateProfileImage(
                        member.getMemberId(), imageUrl);
            }
            redirectAttributes.addFlashAttribute(
                    "message", "회원정보를 수정했습니다.");
            return "redirect:/mypage/business-info";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/business-info/edit";
        }
    }

    @GetMapping("/password")
    public String passwordForm(HttpSession session) {
        return getBusinessMember(session) == null
                ? redirectBySession(session)
                : "mypage/business/password";
    }

    @PostMapping("/password")
    public String changePassword(
            @ModelAttribute MypageDto form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            mypageService.changePassword(
                    member.getMemberId(),
                    form.getCurrentPassword(),
                    form.getNewPassword(),
                    form.getNewPasswordCheck());
            redirectAttributes.addFlashAttribute(
                    "message", "비밀번호를 변경했습니다.");
            return "redirect:/mypage/business-info";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/business-info/password";
        }
    }

    @PostMapping("/approval")
    public String submitApproval(
            @RequestParam("document") MultipartFile document,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            BusinessApplicationDto application =
                    new BusinessApplicationDto();
            application.setDocumentUrl(documentStorage.store(
                    document, member.getMemberId()));
            businessService.submitApplication(
                    member.getMemberId(), application);
            redirectAttributes.addFlashAttribute(
                    "message", "사업자등록증을 제출했습니다. 관리자 승인을 기다려 주세요.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mypage/business-info/approval";
    }

    @GetMapping("/withdraw")
    public String withdrawForm(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        return "mypage/business/withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam("currentPassword") String currentPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            mypageService.verifyCurrentPassword(
                    member.getMemberId(), currentPassword);
            mypageService.withdrawMember(member.getMemberId());
            session.invalidate();
            return "redirect:/";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/business-info/withdraw";
        }
    }

    private MypageDto getBusinessMember(HttpSession session) {
        Long memberId = sessionMemberId(session);
        if (memberId == null) {
            return null;
        }
        MypageDto member = mypageService.getMemberInfo(memberId);
        return member != null
                && Integer.valueOf(2).equals(member.getMemberType())
                ? member : null;
    }

    private String redirectBySession(HttpSession session) {
        return sessionMemberId(session) == null
                ? "redirect:/auth/login" : "redirect:/mypage";
    }

    private Long sessionMemberId(HttpSession session) {
        Object loginMember = session.getAttribute("loginMember");
        return loginMember instanceof LoginMemberDto member
                ? (long) member.getMemberId() : null;
    }
}
