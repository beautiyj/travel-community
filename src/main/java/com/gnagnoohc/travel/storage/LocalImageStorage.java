package com.gnagnoohc.travel.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 개발용 기본 구현. bucket별 로컬 디렉터리에 저장하고 url-prefix 기준 상대경로를 돌려준다.
 * 설정을 안 주면 이 구현이 뜨므로 팀원들의 로컬 실행 방식은 바뀌지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private final StorageProperties properties;

    @Override
    public String store(MultipartFile file, String bucketName) {
        StorageProperties.Bucket bucket = properties.require(bucketName);
        String filename = ImageUploads.newFilename(file, bucket);
        Path directory = bucket.rootPath();
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }
        return bucket.getUrlPrefix() + "/" + filename;
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null) {
            return;
        }
        // 어느 bucket이 만든 주소인지는 url-prefix로 판별한다. 못 찾으면 우리가 만든 주소가 아니므로 넘어간다.
        properties.getBuckets().values().stream()
                .filter(bucket -> storedUrl.startsWith(bucket.getUrlPrefix() + "/"))
                .findFirst()
                .ifPresent(bucket -> deleteWithin(bucket, storedUrl));
    }

    private void deleteWithin(StorageProperties.Bucket bucket, String storedUrl) {
        Path root = bucket.rootPath();
        Path target = root.resolve(storedUrl.substring(bucket.getUrlPrefix().length() + 1)).normalize();
        // DB 값에 ../가 섞여 들어와도 bucket 폴더 밖 파일은 건드리지 않는다
        if (!root.equals(target.getParent())) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("이미지 파일 삭제 실패: {}", storedUrl, e);
        }
    }
}
