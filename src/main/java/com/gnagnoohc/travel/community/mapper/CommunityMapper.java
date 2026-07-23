package com.gnagnoohc.travel.community.mapper;

import java.util.List;
import java.util.Map;

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
	void deleteReplies(int parentId);
	CommentDto selectComment(int commentId);

	// 장소 태그: 이름으로 장소 검색 (방문자인증후기 글쓰기/수정 시 검색 모달에서 사용)
	List<Map<String, Object>> searchPlaces(String keyword);

	// 댓글 권한 체크용: 게시글에 태그된 place의 소유주 member_id (place 미태그 글이면 null)
	Integer selectPlaceOwnerId(int postId);
}