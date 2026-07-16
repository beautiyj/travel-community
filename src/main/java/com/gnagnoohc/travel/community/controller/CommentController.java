package com.gnagnoohc.travel.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.service.CommunityService;

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
	        //   ※ getMemberId 방식은 로그인 담당자의 세션 구조에 맞춰 수정
	        comment.setMemberId(getMemberId(login));
	 
	        service.insertComment(comment);
	 
	        // 작성한 게시글 상세로 되돌아감
	        return "redirect:/community/detail?postId=" + comment.getPostId();
	    }
	  
	  @PostMapping("/community/comment/delete")
	    public String delete(@RequestParam Long commentId,
	                        @RequestParam Long postId,
	                        HttpSession session) {
	 
	        Object login = session.getAttribute("loginMember");
	        if (login == null) {
	            return "redirect:/member/login";
	        }
	 
	        // ※ 본인 댓글인지 확인하려면 댓글 조회 후 memberId 비교 로직 추가 권장
	        service.deleteComment(commentId);
	 
	        return "redirect:/community/detail?postId=" + postId;
	    }
	 
	 
	    // 세션 로그인 정보에서 memberId 꺼내기
	    // ※ CommunityController 와 동일하게 실제 세션 구조에 맞춰 수정
	    private Long getMemberId(Object login) {
	        // 예시) 세션에 회원 객체(MemberDto)를 담는 경우:
	        //   return ((MemberDto) login).getMemberId();
	        return (Long) login;   // ← 실제 구조에 맞춰 수정
	    }
}