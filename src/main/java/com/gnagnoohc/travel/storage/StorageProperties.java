package com.gnagnoohc.travel.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * app.storage.* 설정.
 *
 * 도메인을 추가할 때는 buckets 아래에 이름 하나를 등록하면 되고, 자바 코드는 건드릴 필요가 없다.
 * local/cloudinary 전환은 app.storage.type이 정하는데, 그 값은 각 구현의 @ConditionalOnProperty가
 * 직접 읽으므로 여기서 따로 들고 있지 않는다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** bucket 이름 -> 설정. 이름은 store(file, bucket)에 넘기는 값과 같다. */
    private Map<String, Bucket> buckets = new LinkedHashMap<>();

    /** 등록 안 된 bucket 이름으로 저장을 시도하면 설정 누락이므로 바로 알려준다. */
    public Bucket require(String name) {
        Bucket bucket = buckets.get(name);
        if (bucket == null) {
            throw new IllegalStateException(
                    "app.storage.buckets." + name + " 설정이 없습니다. application.properties를 확인해 주세요.");
        }
        return bucket;
    }

    @Getter
    @Setter
    public static class Bucket {

        /** 로컬 저장 디렉터리 (type=local일 때만 사용) */
        private String dir;

        /** 로컬 서빙 URL 접두사 (예: /uploads/place). 저장된 주소가 이 값으로 시작한다. */
        private String urlPrefix;

        /** Cloudinary 폴더 (type=cloudinary일 때만 사용) */
        private String cloudinaryFolder;

        /** 허용 확장자. 원본 파일명은 신뢰하지 않고 이 목록으로만 검사한다. */
        private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");

        /** 업로드 용량 상한. 0이면 제한 없음(멀티파트 설정에만 의존). */
        private DataSize maxSize = DataSize.ofBytes(0);

        /**
         * true면 이 bucket의 로컬 디렉터리를 urlPrefix로 서빙하는 리소스 핸들러를 자동 등록한다.
         * 이미 자기 도메인에서 핸들러를 등록해 둔 경우 중복되므로 기본값은 false다.
         */
        private boolean serveLocal = false;

        /** dir을 실제 파일 접근에 쓸 절대경로로. 저장·삭제·리소스 핸들러가 같은 기준을 쓰게 모아둔다. */
        public Path rootPath() {
            return Paths.get(dir).toAbsolutePath().normalize();
        }
    }
}
