package com.gnagnoohc.travel.storage;

import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 업로드 파일 검증. Cloudinary는 업로드 전에 걸러야 무료 한도를 낭비하지 않는다.
 */
final class ImageUploads {

    private ImageUploads() {
    }

    /** 빈 파일·용량·확장자를 검사한다. 통과 못 하면 IllegalArgumentException. */
    static void validate(MultipartFile file, StorageProperties.Bucket bucket) {
        extensionOf(file, bucket);
    }

    private static String extensionOf(MultipartFile file, StorageProperties.Bucket bucket) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해 주세요.");
        }
        DataSize maxSize = bucket.getMaxSize();
        if (maxSize != null && maxSize.toBytes() > 0 && file.getSize() > maxSize.toBytes()) {
            throw new IllegalArgumentException(
                    "이미지는 " + (maxSize.toBytes() / (1024 * 1024)) + "MB 이하만 업로드할 수 있습니다.");
        }

        // 원본 파일명은 신뢰하지 않는다. 경로 구분자를 걷어내고 확장자만 화이트리스트로 검사한다.
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
        String name = originalFilename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String extension = (dot >= 0 ? name.substring(dot + 1) : "").toLowerCase(Locale.ROOT);
        if (!bucket.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + originalFilename);
        }
        return extension;
    }
}
