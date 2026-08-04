package com.gnagnoohc.travel.mypage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BusinessDocumentStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "application/pdf", ".pdf");

    private final Path uploadDirectory;

    public BusinessDocumentStorage(
            @Value("${file.upload-auth}")
            String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, Long memberId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "사업자등록증 파일은 5MB 이하만 가능합니다.");
        }

        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException(
                    "사업자등록증은 JPG, PNG, PDF만 업로드할 수 있습니다.");
        }

        String fileName = memberId + "_" + UUID.randomUUID() + extension;
        Path target = uploadDirectory.resolve(fileName).normalize();
        if (!target.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), target,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "사업자등록증 파일을 저장하지 못했습니다.", e);
        }
        return "/uploads/business-documents/" + fileName;
    }

    public Resource load(String documentUrl) {
        if (documentUrl == null || documentUrl.isBlank()) {
            throw new IllegalArgumentException("등록된 사업자등록증이 없습니다.");
        }
        String fileName = documentUrl.substring(
                documentUrl.lastIndexOf('/') + 1);
        Path file = uploadDirectory.resolve(fileName).normalize();
        if (!file.getParent().equals(uploadDirectory)
                || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "사업자등록증 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(file);
    }
}
