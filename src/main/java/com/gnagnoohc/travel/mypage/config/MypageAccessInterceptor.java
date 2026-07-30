package com.gnagnoohc.travel.mypage.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.service.MypageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class MypageAccessInterceptor implements HandlerInterceptor {

    private final MypageService mypageService;

    public MypageAccessInterceptor(MypageService mypageService) {
        this.mypageService = mypageService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        Long memberId = sessionMemberId(session);
        if (memberId == null) {
            response.sendRedirect(
                    request.getContextPath() + "/auth/login");
            return false;
        }

        MypageDto member = mypageService.getMemberInfo(memberId);
        if (member == null) {
            session.invalidate();
            response.sendRedirect(
                    request.getContextPath() + "/auth/login");
            return false;
        }

        if (Integer.valueOf(2).equals(member.getMemberType())) {
            response.sendRedirect(
                    request.getContextPath() + "/business/mypage");
            return false;
        }
        return true;
    }

    private Long sessionMemberId(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object loginMember = session.getAttribute("loginMember");
        if (loginMember instanceof LoginMemberDto member) {
            return (long) member.getMemberId();
        }
        return null;
    }
}
