package com.gnagnoohc.travel.mypage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/business/mypage")
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
        model.addAttribute("places",
                businessService.getPlaces(member.getMemberId()));
        return "mypage/business/dashboard";
    }

    @GetMapping("/info")
    public String info(HttpSession session, Model model) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        model.addAttribute("member", member);
        model.addAttribute("application",
                businessService.getApplication(member.getMemberId()));
        return "mypage/business/info";
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
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        form.setMemberId(member.getMemberId());
        form.setNickname(member.getNickname());
        mypageService.updateMember(form);
        redirectAttributes.addFlashAttribute(
                "message", "회원정보를 수정했습니다.");
        return "redirect:/business/mypage/info";
    }

    @PostMapping("/profile-image")
    public String profileImage(
            @RequestParam("profileImage") MultipartFile profileImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            String imageUrl = mediaStorage.storeProfile(
                    profileImage, member.getMemberId());
            mypageService.updateProfileImage(
                    member.getMemberId(), imageUrl);
            redirectAttributes.addFlashAttribute(
                    "message", "프로필 이미지를 변경했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business/mypage/info";
    }

    @PostMapping("/places/{placeId}/images")
    public String addPlaceImage(
            @PathVariable("placeId") Long placeId,
            @RequestParam("placeImage") MultipartFile placeImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        try {
            String imageUrl = mediaStorage.storePlace(
                    placeImage, placeId);
            businessService.addPlaceImage(
                    member.getMemberId(), placeId, imageUrl);
            redirectAttributes.addFlashAttribute(
                    "message", "사업장 이미지를 등록했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business/mypage";
    }

    @PostMapping("/reapproval")
    public String reapproval(
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
            businessService.resubmit(
                    member.getMemberId(), application);
            redirectAttributes.addFlashAttribute(
                    "message", "사업자 재승인 요청이 접수되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business/mypage/info";
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
    public String withdraw(HttpSession session) {
        MypageDto member = getBusinessMember(session);
        if (member == null) {
            return redirectBySession(session);
        }
        mypageService.withdrawMember(member.getMemberId());
        session.invalidate();
        return "redirect:/";
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
