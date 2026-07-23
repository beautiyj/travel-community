package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.community.dto.CommunityDto;

@Mapper
public interface CommunityMapper {

	List<CommunityDto> selectAll(@Param("category") String category, @Param("q") String q);
	CommunityDto selectOne(int postId);
	void updateReadcount(int postId);
	void insert(CommunityDto dto);
	void update(CommunityDto dto);
	void delete(int postId);
}