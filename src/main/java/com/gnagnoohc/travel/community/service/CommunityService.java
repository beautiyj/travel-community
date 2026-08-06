package com.gnagnoohc.travel.community.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.PlaceTagDto;
import com.gnagnoohc.travel.community.mapper.CommunityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {
	private final CommunityMapper dao;

	private static final int PAGE_SIZE = 10;
	private static final int PAGE_BLOCK = 10; // 페이지 번호를 한 번에 몇 개씩 보여줄지 (이전/다음이 이 단위로 이동)

	// 목록 페이지 조회: 게시글 목록 + 페이지네이션 정보(현재 페이지/총 페이지/번호 블록 범위)를 한 번에 계산해서 반환
	public Map<String, Object> selectPage(String category, String searchType, String q, int page) {
		int totalCount = dao.countAll(category, searchType, q);
		int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);

		int currentPage = Math.max(1, Math.min(page, Math.max(totalPages, 1)));
		int offset = (currentPage - 1) * PAGE_SIZE;
		List<CommunityDto> postList = dao.selectAll(category, searchType, q, offset, PAGE_SIZE);

		int startPage = ((currentPage - 1) / PAGE_BLOCK) * PAGE_BLOCK + 1;
		int endPage = Math.min(startPage + PAGE_BLOCK - 1, totalPages);

		Map<String, Object> result = new HashMap<>();
		result.put("postList", postList);
		result.put("page", currentPage);
		result.put("totalPages", totalPages);
		result.put("startPage", startPage);
		result.put("endPage", endPage);
		return result;
	}

	public CommunityDto selectOne(int postId) {
		return dao.selectOne(postId);
	}

	public void updateReadcount(int postId) {
		dao.updateReadcount(postId);
	}

	public void insert(CommunityDto dto) {
		dao.insert(dto);
	}

	public void update(CommunityDto dto) {
		dao.update(dto);
	}

	public void delete(int postId) {
		dao.delete(postId);
	}

	// 일반후기 다중 장소 태그
	public void insertPlaceTags(int postId, List<Integer> placeIds) {
		dao.insertPlaceTags(postId, placeIds);
	}

	public void deletePlaceTags(int postId) {
		dao.deletePlaceTags(postId);
	}

	public List<PlaceTagDto> selectPlaceTags(int postId) {
		return dao.selectPlaceTags(postId);
	}
}