package com.gnagnoohc.travel.community.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.mapper.CommentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
	private final CommentMapper dao;

	public List<CommentDto> selectComments(int postId) {
		return dao.selectComments(postId);
	}

	public CommentDto selectComment(int commentId) {
		return dao.selectComment(commentId);
	}

	public void insertComment(CommentDto comment) {
		dao.insertComment(comment);
	}

	public void deleteComment(int commentId) {
		dao.deleteComment(commentId);
	}

	public void deleteReplies(int parentId) {
		dao.deleteReplies(parentId);
	}
}