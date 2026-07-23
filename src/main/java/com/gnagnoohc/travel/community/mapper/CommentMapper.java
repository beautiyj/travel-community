package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.community.dto.CommentDto;

@Mapper
public interface CommentMapper {

	List<CommentDto> selectComments(int postId);
	CommentDto selectComment(int commentId);
	void insertComment(CommentDto comment);
	void deleteComment(int commentId);
	void deleteReplies(int parentId);
}