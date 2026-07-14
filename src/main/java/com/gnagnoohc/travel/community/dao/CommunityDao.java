package com.gnagnoohc.travel.community.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.ImageDto;


@Mapper
public interface CommunityDao {

	List<CommunityDto> selectAll(String category, String q);
	void updateReadcount(Long postId);
	CommunityDto selectOne(Long postId);
	List<ImageDto> selectImages(Long postId);
	List<CommentDto> selectComments(Long postId);
	void insert(CommunityDto dto);
	void update(CommunityDto dto);
	void delete(Long postId);
	void insertImage(ImageDto img);
	void insertComment(CommentDto comment);
	void deleteComment(Long commentId);
}
