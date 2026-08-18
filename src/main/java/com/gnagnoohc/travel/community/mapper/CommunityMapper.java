package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.PlaceTagDto;

@Mapper
public interface CommunityMapper {

	List<CommunityDto> selectAll(@Param("category") String category, @Param("searchType") String searchType,
			@Param("q") String q, @Param("offset") int offset, @Param("limit") int limit);
	int countAll(@Param("category") String category, @Param("searchType") String searchType, @Param("q") String q);
	CommunityDto selectOne(int postId);
	void updateReadcount(int postId);
	void insert(CommunityDto dto);
	void update(CommunityDto dto);
	void delete(int postId);

	// 일반후기 다중 장소 태그 (post_place_tag)
	void insertPlaceTags(@Param("postId") int postId, @Param("placeIds") List<Integer> placeIds);
	void deletePlaceTags(int postId);
	List<PlaceTagDto> selectPlaceTags(int postId);
}