package com.gnagnoohc.travel.community.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.mapper.CommonMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommonService {
	private final CommonMapper dao;

	// 장소 태그: 이름으로 장소 검색 (mapper.searchPlaces 그대로 위임) - CommunityController에서 사용
	public List<Map<String, Object>> searchPlaces(String keyword) {
		return dao.searchPlaces(keyword);
	}

	// 장소 태그: 방문자인증후기용 - 로그인 회원의 확정(결제완료) 예약 장소만 검색 - CommunityController에서 사용
	public List<Map<String, Object>> searchConfirmedPlaces(int memberId, String keyword) {
		return dao.searchConfirmedPlaces(memberId, keyword);
	}

	// 댓글 권한 체크용: 게시글에 태그된 place의 소유주 member_id (place 미태그 글이면 null) - CommentController에서 사용
	public Integer selectPlaceOwnerId(int postId) {
		return dao.selectPlaceOwnerId(postId);
	}
}