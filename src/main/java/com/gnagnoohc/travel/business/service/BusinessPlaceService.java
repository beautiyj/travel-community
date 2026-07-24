package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessPlaceDetailDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceRegisterDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceUpdateDto;
//import com.gnagnoohc.travel.business.dto.BusinessRegionOptionDto;
import com.gnagnoohc.travel.business.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessPlaceService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    // member_role='BUSINESS'만 업소를 등록할 수 있는 사업자 회원.
    private static final String MEMBER_ROLE_BUSINESS = "BUSINESS";
    // 수정 폼에서 아직 저장 안 된 새 사진 카드를 나타내는 photoOrder 토큰
    private static final String NEW_PHOTO_TOKEN = "new";

    private final BusinessMapper businessMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // null이면 미등록. 예외를 던지지 않는 조회용 (venue 화면 분기에만 사용)
    public BusinessPlaceOverviewDto findOverview(Long bizMemberId) {
        return businessMapper.selectPlaceOverviewByMember(bizMemberId);
    }

//    public List<BusinessRegionOptionDto> getRegionOptions() {
//        return businessMapper.selectRegionOptions();
//    }

    // venue 화면에서 등록 폼을 보여줄지(사업자) 안내 문구만 보여줄지(일반 유저) 분기용
    public boolean isBusinessMember(Long memberId) {
        String memberRole = businessMapper.selectMemberRole(memberId);
        return MEMBER_ROLE_BUSINESS.equals(memberRole);
    }

    @Transactional
    public void registerPlace(Long bizMemberId, String name, Integer placeType, Long regionId,
                               String address, String description, List<MultipartFile> images) {
        if (!isBusinessMember(bizMemberId)) {
            throw new IllegalStateException("사업자 회원만 업소를 등록할 수 있습니다.");
        }
        if (businessMapper.selectPlaceOverviewByMember(bizMemberId) != null) {
            throw new IllegalStateException("이미 등록된 업소가 있습니다.");
        }

        List<MultipartFile> nonEmpty = images == null ? List.of()
                : images.stream().filter(f -> !f.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            throw new IllegalArgumentException("사진을 최소 1장 등록해야 합니다.");
        }

        List<String> savedUrls = nonEmpty.stream().map(this::saveImageFile).toList();

        BusinessPlaceRegisterDto place = BusinessPlaceRegisterDto.builder()
                .placeType(placeType)
//                .regionId(regionId)
                .memberId(bizMemberId)
                .name(name)
                .description(description)
                .address(address)
//                .mapx(null)
//                .mapy(null)
                .firstImage(savedUrls.get(0))
                .build();

        businessMapper.insertOwnerPlace(place);

        for (int i = 0; i < savedUrls.size(); i++) {
            businessMapper.insertPlaceImage(place.getPlaceId(), savedUrls.get(i), i);
        }
    }

    // venue 읽기뷰/수정폼 공용 상세 조회. venue()에서 findOverview로 이미 존재를 확인한 뒤에만 호출한다.
    public BusinessPlaceDetailDto findDetail(Long bizMemberId) {
        BusinessPlaceDetailDto detail = businessMapper.selectPlaceDetailByMember(bizMemberId);
        detail.setImages(businessMapper.selectPlaceImages(detail.getPlaceId()));
        return detail;
    }

    @Transactional
    public void updatePlace(Long bizMemberId, String name, Integer placeType, Long regionId,
                             String address, String description, List<String> photoOrder,
                             List<String> removeImageUrls, List<MultipartFile> newImages) {
        BusinessPlaceOverviewDto overview = businessMapper.selectPlaceOverviewByMember(bizMemberId);
        if (overview == null) {
            throw new IllegalStateException("등록된 업소가 없습니다.");
        }
        Long placeId = overview.getPlaceId();

        List<String> currentImages = businessMapper.selectPlaceImages(placeId);
        Set<String> currentImageSet = Set.copyOf(currentImages);
        Set<String> toRemove = removeImageUrls == null ? Set.of() : Set.copyOf(removeImageUrls);

        List<MultipartFile> nonEmptyNewImages = newImages == null ? List.of()
                : newImages.stream().filter(f -> !f.isEmpty()).toList();
        List<String> newUrls = nonEmptyNewImages.stream().map(this::saveImageFile).toList();

        // photoOrder는 기존/신규 카드를 한 그리드에서 드래그로 섞은 최종 순서.
        // 기존 사진은 URL 그대로, 신규 사진은 "new" 토큰으로 오고, 토큰이 나온 순서대로 newUrls를 하나씩 매칭한다.
        // (편집 폼 JS가 새 카드의 실제 파일 순서를 항상 newImages 제출 순서와 동일하게 맞춰두기 때문에 가능한 매칭)
        List<String> tokens = (photoOrder == null) ? currentImages : photoOrder;
        List<String> finalOrder = new ArrayList<>();
        int newIndex = 0;
        for (String token : tokens) {
            if (NEW_PHOTO_TOKEN.equals(token)) {
                if (newIndex < newUrls.size()) {
                    finalOrder.add(newUrls.get(newIndex++));
                }
            } else if (currentImageSet.contains(token) && !toRemove.contains(token)) {
                finalOrder.add(token);
            }
        }
        // JS가 비활성화된 등 photoOrder에 신규 사진이 하나도 안 실려온 경우를 대비해 남은 신규 사진은 뒤에 붙인다
        while (newIndex < newUrls.size()) {
            finalOrder.add(newUrls.get(newIndex++));
        }

        if (finalOrder.isEmpty()) {
            throw new IllegalArgumentException("사진을 최소 1장 유지해야 합니다.");
        }
        if (finalOrder.size() > 5) {
            throw new IllegalArgumentException("사진은 최대 5장까지 등록할 수 있습니다.");
        }

        // 순서 변경(드래그)까지 반영해야 해서 기존 행은 전부 지우고 최종 순서대로 다시 넣는다
        for (String url : currentImages) {
            businessMapper.deletePlaceImage(placeId, url);
        }
        for (int i = 0; i < finalOrder.size(); i++) {
            businessMapper.insertPlaceImage(placeId, finalOrder.get(i), i);
        }

        // sort_order 0(대표사진)은 최종 순서의 첫 번째 사진 -> 사이드바 아바타에도 그대로 쓰인다
        String firstImage = finalOrder.get(0);

        BusinessPlaceUpdateDto update = BusinessPlaceUpdateDto.builder()
                .placeId(placeId)
                .memberId(bizMemberId)
                .name(name)
                .placeType(placeType)
//                .regionId(regionId)
                .address(address)
                .description(description)
                .firstImage(firstImage)
                .build();

        if (businessMapper.updatePlace(update) == 0) {
            throw new IllegalStateException("업소 정보를 수정할 권한이 없습니다.");
        }
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
