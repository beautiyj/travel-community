package com.gnagnoohc.travel.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * app.storage.* 설정.
 * 도메인을 추가할 때는 buckets 아래에 이름 하나를 등록하면 되고, 자바 코드는 건드릴 필요가 없다.
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

        /** Cloudinary 폴더 */
        private String cloudinaryFolder;

        /** 허용 확장자. 원본 파일명은 신뢰하지 않고 이 목록으로만 검사한다. */
        private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");

        /** 업로드 용량 상한. 0이면 제한 없음(멀티파트 설정에만 의존). */
        private DataSize maxSize = DataSize.ofBytes(0);
    }
}
