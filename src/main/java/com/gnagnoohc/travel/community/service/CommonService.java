package com.gnagnoohc.travel.community.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.dto.PlaceTagDto;
import com.gnagnoohc.travel.community.mapper.CommonMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommonService {
	private final CommonMapper dao;

	// 장소 태그 검색 결과 페이지 크기 ("더보기" 버튼 단위)
	private static final int PLACE_SEARCH_PAGE_SIZE = 10;

	// 장소 태그: 이름으로 장소 검색 (mapper.searchPlaces 그대로 위임) - CommunityController에서 사용
	// page는 0부터 시작. "더보기" 여부 판단을 위해 PAGE_SIZE+1건을 가져와 잘라냄 (별도 count 쿼리 없이 처리)
	public Map<String, Object> searchPlaces(String keyword, int page) {
		int offset = page * PLACE_SEARCH_PAGE_SIZE;
		List<PlaceTagDto> rows = dao.searchPlaces(keyword, offset, PLACE_SEARCH_PAGE_SIZE + 1);
		return toPageResult(rows);
	}

	// 장소 태그: 방문자인증후기용 - 로그인 회원의 확정(결제완료) 예약 장소만 검색 - CommunityController에서 사용
	public Map<String, Object> searchConfirmedPlaces(int memberId, String keyword, int page) {
		int offset = page * PLACE_SEARCH_PAGE_SIZE;
		List<PlaceTagDto> rows = dao.searchConfirmedPlaces(memberId, keyword, offset, PLACE_SEARCH_PAGE_SIZE + 1);
		return toPageResult(rows);
	}

	// PAGE_SIZE+1건 조회 결과를 {"items": 실제 PAGE_SIZE건, "hasMore": 다음 페이지 존재 여부}로 가공
	private Map<String, Object> toPageResult(List<PlaceTagDto> rows) {
		boolean hasMore = rows.size() > PLACE_SEARCH_PAGE_SIZE;
		List<PlaceTagDto> items = hasMore ? rows.subList(0, PLACE_SEARCH_PAGE_SIZE) : rows;

		Map<String, Object> result = new HashMap<>();
		result.put("items", items);
		result.put("hasMore", hasMore);
		return result;
	}

	// 댓글 권한 체크용: 게시글에 태그된 place의 소유주 member_id (place 미태그 글이면 null) - CommentController에서 사용
	public Integer selectPlaceOwnerId(int postId) {
		return dao.selectPlaceOwnerId(postId);
	}
}