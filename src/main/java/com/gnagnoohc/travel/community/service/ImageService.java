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

	 * removeImageUrls는 클라이언트가 보낸 값이라 그대로 신뢰하지 않는다. deleteImageByUrl이
	 * postId 소속으로 실제 삭제한 행이 있을 때만(반환값 > 0) 스토리지까지 지운다 — 그렇지 않으면
	 * 이 글 소유자가 아무 Cloudinary url이나 넣어 다른 글/프로필/업소 사진을 지울 수 있게 된다.
	 */
	public void removeImages(int postId, List<String> removeImageUrls) {
		if (removeImageUrls == null || removeImageUrls.isEmpty()) return;

		for (String url : removeImageUrls) {
			if (url == null || url.isBlank()) continue;
			int deleted = dao.deleteImageByUrl(postId, url);
			if (deleted > 0) {
				deleteStorageQuietly(url);
			}
		}
	}

	/**
	 * 게시글 삭제 전에 호출. post 테이블 삭제에 딸린 FK CASCADE로 post_image 행이 없어지기 전에
	 * 그 글의 이미지 URL 목록만 미리 읽어둔다. 부수효과가 없으므로 이 호출 자체는 실패해도
	 * 안전하다 — 실제 Cloudinary 삭제는 post 행 삭제가 성공한 뒤 deleteFromStorage()로 한다.
	 */
	public List<String> collectImageUrls(int postId) {
		return selectImages(postId).stream()
				.map(ImageDto::getImageUrl)
				.toList();
	}

	/**
	 * post 행 삭제가 성공적으로 끝난 뒤에만 호출해야 한다. Cloudinary 삭제는 롤백으로 되돌릴 수
	 * 없으므로, DB 삭제가 먼저 확정된 걸 확인한 다음에 지워야 "게시글은 남았는데 사진만 사라진"
	 * 상태를 피할 수 있다.
	 */
	public void deleteFromStorage(List<String> urls) {
		urls.forEach(this::deleteStorageQuietly);
	}

	private void deleteStorageQuietly(String url) {
		try {
			imageStorage.delete(url);
		} catch (RuntimeException e) {
			log.warn("게시글 이미지 삭제 실패: {}", url, e);
		}
	}
}
