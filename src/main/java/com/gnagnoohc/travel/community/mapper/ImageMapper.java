package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.community.dto.ImageDto;

@Mapper
public interface ImageMapper {

	List<ImageDto> selectImages(int postId);
	void insertImage(ImageDto img);
	void deleteImageByUrl(@Param("postId") int postId, @Param("imageUrl") String imageUrl);
	void deleteImagesByPostId(int postId);
}