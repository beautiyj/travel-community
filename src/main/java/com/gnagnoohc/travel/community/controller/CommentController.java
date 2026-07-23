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

		// 사장님(business) 권한 체크
		// - place가 태그된 리뷰 글일 때만 검증 (place 미태그 일반 글은 사장님도 자유롭게 댓글 가능)
		// - 태그된 글이면, 그 place의 소유주(member_id)가 본인일 때만 댓글 작성 허용
		if ("BUSINESS".equalsIgnoreCase(getMemberRole(login))) {
			Integer placeOwnerId = service.selectPlaceOwnerId(comment.getPostId());
			if (placeOwnerId != null && placeOwnerId != getMemberId(login)) {
				// 본인 업소의 리뷰가 아니면 등록 거부 → 안내 모달 띄우기 위한 파라미터
				return "redirect:/community/detail?postId=" + comment.getPostId() + "&commentDenied=true";
			}
		}

		service.insertComment(comment);

		// 작성한 게시글 상세로 되돌아감
		return "redirect:/community/detail?postId=" + comment.getPostId();
	}

	@PostMapping("/community/comment/delete")
	public String delete(@RequestParam("commentId") int commentId, @RequestParam("postId") int postId, HttpSession session) {

		Object login = session.getAttribute("loginMember");
		if (login == null) {
			return "redirect:/member/login";
		}

		// 본인 댓글인지 확인
		// ※ service.selectComment(int)이 아직 없다면 Mapper/Dao/Service에 단건 조회 추가 필요
		CommentDto comment = service.selectComment(commentId);
		if (!isOwner(comment, login)) {
			return "redirect:/community/detail?postId=" + postId;
		}

		// 원댓글(parentId 없음)을 지우는 경우, 딸린 대댓글부터 먼저 삭제
		// (대댓글 작성자가 다를 수 있어도 원댓글이 사라지면 함께 지우는 게 일반적인 정책)
		if (comment.getParentId() == null) {
			service.deleteReplies(commentId);
		}

		service.deleteComment(commentId);

		return "redirect:/community/detail?postId=" + postId;
	}

	// 로그인한 사람이 댓글 작성자인지 확인
	private boolean isOwner(CommentDto comment, Object login) {
		if (login == null || comment == null)
			return false;
		return comment.getMemberId() == getMemberId(login);
	}

	// 세션 로그인 정보에서 memberId 꺼내기
	// session.setAttribute("loginMember", loginResult.loginMember())로
	// LoginMemberDto(memberId: int, nickname, memberRole)가 세션에 그대로 들어옴
	// ※ LoginMemberDto.memberId는 int, 커뮤니티 쪽도 이제 int라서 타입 그대로 맞음
	private int getMemberId(Object login) {
		return ((LoginMemberDto) login).getMemberId();
	}

	// 세션 로그인 정보에서 memberRole 꺼내기 ('USER' / 'BUSINESS' / 'ADMIN')
	private String getMemberRole(Object login) {
		return ((LoginMemberDto) login).getMemberRole();
	}
}