package com.gnagnoohc.travel.mypage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BusinessMediaStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path rootDirectory;

    public BusinessMediaStorage(
            @Value("${app.upload.media-dir:uploads/media}")
            String rootDirectory) {
        this.rootDirectory =
                Paths.get(rootDirectory).toAbsolutePath().normalize();
    }

    public String storeProfile(MultipartFile file, Long memberId) {
        return store(file, "profiles", "member-" + memberId);
    }

    public String storePlace(MultipartFile file, Long placeId) {
        return store(file, "places", "place-" + placeId);
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    private String store(
            MultipartFile file, String folder, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "이미지는 5MB 이하만 업로드할 수 있습니다.");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException(
                    "JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }

        Path directory = rootDirectory.resolve(folder).normalize();
        String fileName = prefix + "-" + UUID.randomUUID() + extension;
        Path target = directory.resolve(fileName).normalize();
        if (!target.getParent().equals(directory)) {
            throw new IllegalArgumentException("올바르지 않은 파일 경로입니다.");
        }
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", e);
        }
        return "/uploads/media/" + folder + "/" + fileName;
    }
}
