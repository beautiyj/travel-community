package com.gnagnoohc.travel.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 도메인 공용 이미지 저장소(Cloudinary). 각 도메인은 이 인터페이스만 주입받는다.
 * bucket 등록 방법은 application.properties의 app.storage.buckets 주석 참고.
 */
public interface ImageStorage {

    /**
     * 업로드 파일을 저장하고 DB에 넣을 이미지 주소를 돌려준다.
     * 반환값은 JSP에서 {@code <img src="${url}">}로 바로 쓸 수 있는 https:// 절대 URL이다.
     *
     * @param bucket app.storage.buckets 아래에 등록한 이름 (예: "place")
     */
    String store(MultipartFile file, String bucket);

    /**
     * store()가 돌려줬던 주소의 이미지를 지운다.
     * 이미 없거나 현재 저장소가 만든 주소가 아니면 조용히 넘어간다.
     *
     * 저장소를 바꾼 뒤에는 이전 방식으로 저장된 주소가 DB에 남아있을 수 있는데,
     * 그런 주소는 대상이 아니므로 무시한다(잘못 지우는 것보다 안전하다).
     */
    void delete(String storedUrl);
}
