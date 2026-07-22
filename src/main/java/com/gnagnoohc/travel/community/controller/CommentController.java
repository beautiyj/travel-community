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
		
		// TODO: post.category == "방문자인증 후기" 인 글이면 댓글 권한 제한 필요
        //   - 대상: 해당 place_id의 사장님 + 일반 유저만 댓글 가능
        //   - PLACE.member_id / admin_type 값 의미가 아직 안 정해져서 보류 중
        //     (API 등록 장소는 진짜 사장님이 없는데 member_id가 null인지,
        //      admin_type으로 API/직접등록 여부를 구분해야 하는지 등)
        //   - 정해지면: post 조회 → place 조회 → place.member_id로 사장님 여부 판별 후 검증

		service.insertComment(comment);

		// 작성한 게시글 상세로 되돌아감
		return "redirect:/community/detail?postId=" + comment.getPostId();
	}

	@PostMapping("/community/comment/delete")
	public String delete(@RequestParam int commentId, @RequestParam int postId, HttpSession session) {

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
}