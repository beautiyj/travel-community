package com.gnagnoohc.travel.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * admin 컨트롤러가 공통으로 사용하는 로그인 세션 관리자 검사입니다.
 * 세션 검사는 빠른 접근 차단용이며, 실제 관리자 상태는 서비스에서 DB로 다시 확인합니다.
 */
final class AdminSessionSupport {

	private AdminSessionSupport() {
	}

	static LoginMemberDto requireAdmin(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null
				|| !(session.getAttribute("loginMember")
						instanceof LoginMemberDto loginMember)) {
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		if (loginMember.getMemberType() != 0
				|| !"ADMIN".equals(loginMember.getMemberRole())) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
		}
		return loginMember;
	}
}
