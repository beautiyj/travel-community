package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.gnagnoohc.travel.community.dto.ImageDto;

@Mapper
public interface ImageMapper {

	List<ImageDto> selectImages(int postId);
	void insertImage(ImageDto img);
}