package com.gnagnoohc.travel.mypage.service;

import com.gnagnoohc.travel.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BusinessDocumentStorage {

    // app.storage.buckets.business-document 설정을 쓰는 사업자등록증 bucket.
    // 회원가입 시 제출본(AuthService)과 같은 bucket이라 관리자 열람 쪽이 두 경로를 똑같이 처리한다.
    private static final String DOCUMENT_BUCKET = "business-document";

    private final ImageStorage imageStorage;

    public String store(MultipartFile file, Long memberId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return imageStorage.store(file, DOCUMENT_BUCKET);
    }
}
