package com.gnagnoohc.travel.admin.service;

import com.gnagnoohc.travel.admin.dto.AdminPlaceOverviewDto;
import com.gnagnoohc.travel.admin.dto.AdminPlaceRegisterDto;
import com.gnagnoohc.travel.admin.dto.AdminRegionOptionDto;
import com.gnagnoohc.travel.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminPlaceService {

    private static final String CONTENT_ID_PREFIX = "OWN-";
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    // member_type=0만 업소를 등록할 수 있는 사업자 회원. 그 외는 일반 유저.
    private static final int MEMBER_TYPE_BUSINESS = 0;

    private final AdminMapper adminMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // null이면 미등록. 예외를 던지지 않는 조회용 (venue 화면 분기에만 사용)
    public AdminPlaceOverviewDto findOverview(Long bizMemberId) {
        return adminMapper.selectPlaceOverviewByMember(bizMemberId);
    }

    public List<AdminRegionOptionDto> getRegionOptions() {
        return adminMapper.selectRegionOptions();
    }

    // venue 화면에서 등록 폼을 보여줄지(사업자) 안내 문구만 보여줄지(일반 유저) 분기용
    public boolean isBusinessMember(Long memberId) {
        Integer memberType = adminMapper.selectMemberType(memberId);
        return memberType != null && memberType == MEMBER_TYPE_BUSINESS;
    }

    @Transactional
    public void registerPlace(Long bizMemberId, String name, Integer placeType, Long regionId,
                               String address, String description, List<MultipartFile> images) {
        if (!isBusinessMember(bizMemberId)) {
            throw new IllegalStateException("사업자 회원만 업소를 등록할 수 있습니다.");
        }
        if (adminMapper.selectPlaceOverviewByMember(bizMemberId) != null) {
            throw new IllegalStateException("이미 등록된 업소가 있습니다.");
        }

        List<MultipartFile> nonEmpty = images == null ? List.of()
                : images.stream().filter(f -> !f.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            throw new IllegalArgumentException("사진을 최소 1장 등록해야 합니다.");
        }

        List<String> savedUrls = nonEmpty.stream().map(this::saveImageFile).toList();

        AdminPlaceRegisterDto place = AdminPlaceRegisterDto.builder()
                .contentId(generateOwnerContentId())
                .placeType(placeType)
                .regionId(regionId)
                .memberId(bizMemberId)
                .name(name)
                .description(description)
                .address(address)
                .mapx(null)
                .mapy(null)
                .firstImage(savedUrls.get(0))
                .adminType(AdminPlaceRegisterDto.ADMIN_TYPE_OWNER_REGISTERED)
                .build();

        adminMapper.insertOwnerPlace(place);

        for (int i = 0; i < savedUrls.size(); i++) {
            adminMapper.insertPlaceImage(place.getPlaceId(), savedUrls.get(i), i);
        }
    }

    // content_id VARCHAR(20) UNIQUE NOT NULL. "OWN-"(4) + 16 hex chars = 20자, 정확히 꽉 채움
    private String generateOwnerContentId() {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return CONTENT_ID_PREFIX + randomPart;
    }

    // 원본 파일명은 신뢰하지 않고 확장자만 화이트리스트로 뽑아 서버측 생성 파일명(UUID)으로 저장
    private String saveImageFile(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            file.transferTo(uploadPath.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }
        return "/uploads/place/" + filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
        String name = originalFilename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String extension = (dot >= 0 ? name.substring(dot + 1) : "").toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + originalFilename);
        }
        return extension;
    }
}
