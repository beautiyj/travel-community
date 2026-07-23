package com.gnagnoohc.travel.community.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.mapper.CommunityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {
	private final CommunityMapper dao;

	public List<CommunityDto> selectAll(String category, String q) {
		return dao.selectAll(category, q);
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
}