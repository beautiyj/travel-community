package com.gnagnoohc.travel.mypage.service;

import com.gnagnoohc.travel.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BusinessMediaStorage {

    private static final String PROFILE_BUCKET = "profile";
    private static final String PLACE_BUCKET = "place-media";

    private final ImageStorage imageStorage;

    public String storeProfile(MultipartFile file, Long memberId) {
        return imageStorage.store(file, PROFILE_BUCKET);
    }

    public String storePlace(MultipartFile file, Long placeId) {
        return imageStorage.store(file, PLACE_BUCKET);
    }
}
