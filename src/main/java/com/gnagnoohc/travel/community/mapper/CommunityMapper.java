package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.ImageDto;


@Mapper
public interface CommunityMapper {

	List<CommunityDto> selectAll(@Param("category") String category, @Param("q") String q);
	void updateReadcount(int postId);
	CommunityDto selectOne(int postId);
	List<ImageDto> selectImages(int postId);
	List<CommentDto> selectComments(int postId);
	void insert(CommunityDto dto);
	void update(CommunityDto dto);
	void delete(int postId);
	void insertImage(ImageDto img);
	void insertComment(CommentDto comment);
	void deleteComment(int commentId);
	CommentDto selectComment(int commentId);
}