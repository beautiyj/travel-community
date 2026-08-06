package com.gnagnoohc.travel.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gnagnoohc.travel.community.dto.ImageDto;
import com.gnagnoohc.travel.community.mapper.ImageMapper;
import com.gnagnoohc.travel.storage.ImageStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
	// app.storage.buckets.community 설정을 쓰는 게시글 첨부 이미지 bucket
	private static final String IMAGE_BUCKET = "community";

	private final ImageMapper dao;
	private final ImageStorage imageStorage;

	public List<ImageDto> selectImages(int postId) {
		return dao.selectImages(postId);
	}

	public void insertImage(ImageDto img) {
		dao.insertImage(img);
	}

	// 이미지 파일들을 Cloudinary에 저장하고 URL을 DB(IMAGE 테이블)에 기록
	// CommunityController.write() / update() 에서 게시글 저장 후(postId 확정 시점) 호출
	public void saveImages(MultipartFile[] images, int postId) {
		if (images == null) return;

		List<ImageDto> existingImages = selectImages(postId);
		int order = existingImages.size();   // 이어서 매길 시작 번호

		for (MultipartFile image : images) {
			if (image == null || image.isEmpty()) continue;

			String storedUrl = imageStorage.store(image, IMAGE_BUCKET);

			ImageDto img = new ImageDto();
			img.setPostId(postId);        // FK 컬럼명이 post_id
			img.setImageUrl(storedUrl);    // Cloudinary가 돌려준 https 절대 URL
			img.setSortOrder(order++);     // 정렬 순서
			insertImage(img);
		}
	}

	/**
	 * 수정 화면에서 본문 편집 중 빠진 기존 이미지를 정리한다. DB 행을 먼저 지우고 나서
	 * 저장소 파일을 지운다 — 저장소 삭제가 실패해도 화면에는 이미 안 보이는 게 맞고,
	 * 반대 순서면 파일은 지웠는데 DB 삭제가 실패해 깨진 이미지 링크가 남을 수 있다.
	 * 트랜잭션 안에 넣지 않는다: 저장소 삭제는 롤백으로 되돌릴 수 없다.
	 */
	public void removeImages(int postId, List<String> removeImageUrls) {
		if (removeImageUrls == null || removeImageUrls.isEmpty()) return;

		for (String url : removeImageUrls) {
			if (url == null || url.isBlank()) continue;
			dao.deleteImageByUrl(postId, url);
			deleteStorageQuietly(url);
		}
	}

	/**
	 * 게시글을 통째로 삭제할 때 호출. post 테이블 삭제에 딸린 FK CASCADE로 post_image 행이
	 * 먼저 없어지기 전에, 그 글의 이미지 URL 목록을 미리 읽어 Cloudinary 쪽을 지운다.
	 * post_image 행 삭제는 CASCADE 여부와 무관하게 여기서도 한 번 더 해준다(중복 삭제라 안전).
	 */
	public void removeAllImages(int postId) {
		List<ImageDto> images = selectImages(postId);
		for (ImageDto img : images) {
			deleteStorageQuietly(img.getImageUrl());
		}
		dao.deleteImagesByPostId(postId);
	}

	private void deleteStorageQuietly(String url) {
		try {
			imageStorage.delete(url);
		} catch (RuntimeException e) {
			log.warn("게시글 이미지 삭제 실패: {}", url, e);
		}
	}
}
