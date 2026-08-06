package com.gnagnoohc.travel.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryImageStorage implements ImageStorage {

    private static final String UPLOAD_SEGMENT = "/upload/";

    /** 업로드 응답 URL 앞에 붙는 버전 세그먼트(v1712345678). public_id의 일부가 아니다. */
    private static final Pattern VERSION_SEGMENT = Pattern.compile("^v\\d+/");

    private final Cloudinary cloudinary;
    private final StorageProperties properties;

    @Override
    public String store(MultipartFile file, String bucketName) {
        StorageProperties.Bucket bucket = properties.require(bucketName);
        // 확장자·용량은 로컬과 동일하게 먼저 걸러 업로드 자체를 막는다(무료 한도 낭비 방지)
        ImageUploads.validate(file, bucket);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", bucket.getCloudinaryFolder(),
                    "public_id", UUID.randomUUID().toString(),
                    "resource_type", "image",
                    "overwrite", false));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String storedUrl) {
        String publicId = toPublicId(storedUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
        } catch (IOException e) {
            log.warn("Cloudinary 이미지 삭제 실패: {}", storedUrl, e);
        }
    }

    // store()가 돌려준 secure_url을 거꾸로 훑어 Cloudinary가 파일을 식별하는 public_id를 뽑는다.
    // 우리 저장소가 만든 주소가 아니면 null을 돌려주고 호출부는 그냥 넘어간다.
    private String toPublicId(String storedUrl) {
        if (storedUrl == null) {
            return null;
        }
        int uploadAt = storedUrl.indexOf(UPLOAD_SEGMENT);
        if (uploadAt < 0) {
            return null;
        }
        String path = VERSION_SEGMENT.matcher(storedUrl.substring(uploadAt + UPLOAD_SEGMENT.length()))
                .replaceFirst("");

        // 확장자만 떼어낸다. 폴더명에 점이 있을 수 있으므로 마지막 세그먼트 안에서만 찾는다
        int dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
    }
}
