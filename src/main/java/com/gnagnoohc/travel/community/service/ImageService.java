package com.gnagnoohc.travel.community.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gnagnoohc.travel.community.dto.ImageDto;
import com.gnagnoohc.travel.community.mapper.ImageMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {
	private final ImageMapper dao;

	@Value("${file.upload-community}")
	private String uploadDir;

	public List<ImageDto> selectImages(int postId) {
		return dao.selectImages(postId);
	}

	public void insertImage(ImageDto img) {
		dao.insertImage(img);
	}

	// 이미지 파일들을 디스크에 저장하고 경로를 DB(IMAGE 테이블)에 기록
	// CommunityController.write() / update() 에서 게시글 저장 후(postId 확정 시점) 호출
	// (기존 CommunityController의 private saveImages()를 그대로 옮김)
	public void saveImages(MultipartFile[] images, int postId) throws IOException {
		if (images == null) return;

		List<ImageDto> existingImages = selectImages(postId);
		int order = existingImages.size();   // 이어서 매길 시작 번호

		for (MultipartFile image : images) {
			if (image == null || image.isEmpty()) continue;

			// 파일명 중복 방지: UUID + 원본이름
			String savedName = UUID.randomUUID() + "_" + image.getOriginalFilename();

			File folder = new File(uploadDir);
			if (!folder.exists()) folder.mkdirs();       // 폴더 없으면 생성

			image.transferTo(new File(folder, savedName));

			ImageDto img = new ImageDto();
			img.setPostId(postId);        // FK 컬럼명이 post_id
			img.setImageUrl(savedName);    // 저장된 파일명
			img.setSortOrder(order++);     // 정렬 순서
			insertImage(img);
		}
	}
}