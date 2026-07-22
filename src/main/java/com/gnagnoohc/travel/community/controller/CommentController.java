package com.gnagnoohc.travel.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.service.CommunityService;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentController {
	private final CommunityService service;

	// 댓글, 대댓글 등록
	@PostMapping("/community/comment/write")
	public String write(CommentDto comment, HttpSession session) {

		// 로그인 확인
		Object login = session.getAttribute("loginMember");
		if (login == null) {
			return "redirect:/member/login";
		}

		// 작성자(memberId) 세팅
		// ※ getMemberId 방식은 로그인 담당자의 세션 구조에 맞춰 수정
		comment.setMemberId(getMemberId(login));

		service.insertComment(comment);

		// 작성한 게시글 상세로 되돌아감
		return "redirect:/community/detail?postId=" + comment.getPostId();
	}

	@PostMapping("/community/comment/delete")
	public String delete(@RequestParam Long commentId, @RequestParam Long postId, HttpSession session) {

		Object login = session.getAttribute("loginMember");
		if (login == null) {
			return "redirect:/member/login";
		}

		// 본인 댓글인지 확인
		// ※ service.selectComment(Long)이 아직 없다면 Mapper/Dao/Service에 단건 조회 추가 필요
		CommentDto comment = service.selectComment(commentId);
		if (!isOwner(comment, login)) {
			return "redirect:/community/detail?postId=" + postId;
		}

		service.deleteComment(commentId);

		return "redirect:/community/detail?postId=" + postId;
	}

	// 로그인한 사람이 댓글 작성자인지 확인
	private boolean isOwner(CommentDto comment, Object login) {
		if (login == null || comment == null)
			return false;
		return comment.getMemberId().equals(getMemberId(login));
	}

	// 세션 로그인 정보에서 memberId 꺼내기
	// session.setAttribute("loginMember", loginResult.loginMember())로
	// LoginMemberDto(memberId: int, nickname, memberRole)가 세션에 그대로 들어옴
	// ※ LoginMemberDto.memberId는 int라서 커뮤니티 쪽 Long과 안 맞음 → 명시적으로 변환
	private Long getMemberId(Object login) {
		return (long) ((LoginMemberDto) login).getMemberId();
	}
}