package com.gnagnoohc.travel.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 도메인 공용 이미지 저장소.
 *
 * 각 도메인은 이 인터페이스만 주입받고, 실제 저장 위치(로컬 디스크 / Cloudinary)는
 * app.storage.type 설정 한 줄로 결정된다. 도메인 코드는 어느 쪽인지 알 필요가 없다.
 * bucket 등록 방법은 application.properties의 app.storage.buckets 주석 참고.
 *
 * 주의: 저장된 이미지는 URL만 알면 누구나 볼 수 있다.
 * 사업자등록증처럼 비공개여야 하는 파일은 접근 제어가 있는 별도 경로로 저장해야 한다.
 */
public interface ImageStorage {

    /**
     * 업로드 파일을 저장하고 DB에 넣을 이미지 주소를 돌려준다.
     * 반환값은 JSP에서 {@code <img src="${url}">}로 바로 쓸 수 있는 형태다
     * (로컬이면 /uploads/... 상대경로, Cloudinary면 https:// 절대 URL).
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
