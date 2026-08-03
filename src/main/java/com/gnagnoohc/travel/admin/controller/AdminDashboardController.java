package com.gnagnoohc.travel.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.gnagnoohc.travel.admin.service.AdminBusinessApplicationService;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

	private final AdminBusinessApplicationService service;

	@GetMapping
	public String dashboard(HttpServletRequest request, Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		try {
			model.addAttribute(
					"summary",
					service.getDashboardSummary(admin.getMemberId()));
		} catch (SecurityException e) {
			// 세션이 관리자여도 DB에서 정지·탈퇴 또는 권한 변경된 경우 접근을 거부합니다.
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"현재 사용자는 관리자 기능을 사용할 수 없습니다.",
					e);
		}
		return "admin/dashboard";
	}
}
