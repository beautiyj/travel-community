package com.gnagnoohc.travel.community.controller;

import org.springframework.stereotype.Controller;

import com.gnagnoohc.travel.community.service.CommunityService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentController {
	private final CommunityService service;
	
	// 댓글, 대댓글 등록
	
	// 댓글 삭제
}