package com.gnagnoohc.travel.community.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.ImageDto;
import com.gnagnoohc.travel.community.mapper.CommunityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {
	private final CommunityMapper dao;

	public List<CommunityDto> selectAll(String category, String q) {
		return dao.selectAll(category, q);
	}

	public void updateReadcount(Long postId) {
		dao.updateReadcount(postId);
	}

	public CommunityDto selectOne(Long postId) {
		return dao.selectOne(postId);
	}

	public List<ImageDto> selectImages(Long postId) {
		return dao.selectImages(postId);
	}

	public List<CommentDto> selectComments(Long postId) {
		return dao.selectComments(postId);
	}

	public void insert(CommunityDto dto) {
		dao.insert(dto);
	}

	public void update(CommunityDto dto) {
		dao.update(dto);
	}

	public void delete(Long postId) {
		dao.delete(postId);
	}

	public void insertImage(ImageDto img) {
		dao.insertImage(img);
	}

	public void insertComment(CommentDto comment) {
		dao.insertComment(comment);
	}

	public void deleteComment(Long commentId) {
		dao.deleteComment(commentId);
	}

	public CommentDto selectComment(Long commentId) {
	    return dao.selectComment(commentId);
	}
	
}
