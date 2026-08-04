package com.gnagnoohc.travel.business.service;

import com.gnagnoohc.travel.business.dto.BusinessPlaceDetailDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceFormDto;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessPlaceService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    // member_role='BUSINESS'만 업소를 등록할 수 있는 사업자 회원.
    private static final String MEMBER_ROLE_BUSINESS = "BUSINESS";
    // 수정 폼에서 아직 저장 안 된 새 사진 카드를 나타내는 photoOrder 토큰
    private static final String NEW_PHOTO_TOKEN = "new";
    // 가격 설정 대상은 숙박(place_type='stay')뿐. 맛집/관광지는 예약금을 받지 않는다.
    private static final String PLACE_TYPE_LODGING = "stay";
    private static final String PRICE_TYPE_FIXED = "FIXED";
    private static final String PRICE_TYPE_VARIABLE = "VARIABLE";
    private static final String PRICE_TYPE_FREE = "FREE";
    private static final Set<String> ALLOWED_PRICE_TYPES =
            Set.of(PRICE_TYPE_FIXED, PRICE_TYPE_VARIABLE, PRICE_TYPE_FREE);

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
    public void registerPlace(Long bizMemberId, BusinessPlaceFormDto form, List<MultipartFile> images) {
        requireRequiredFields(form);
        if (!isBusinessMember(bizMemberId)) {
            throw new IllegalStateException("사업자 회원만 업소를 등록할 수 있습니다.");
        }
        if (businessMapper.selectPlaceOverviewByMember(bizMemberId) != null) {
            throw new IllegalStateException("이미 등록된 업소가 있습니다.");
        }

        List<MultipartFile> nonEmpty = toNonEmpty(images);
        if (nonEmpty.isEmpty()) {
            throw new IllegalArgumentException("사진을 최소 1장 등록해야 합니다.");
        }

        PriceSetting price = resolvePrice(form);

        List<String> savedUrls = nonEmpty.stream().map(this::saveImageFile).toList();

        BusinessPlaceRegisterDto place = BusinessPlaceRegisterDto.builder()
                .placeType(form.getPlaceType())
                .priceType(price.priceType())
                .minPrice(price.minPrice())
//                .regionId(form.getRegionId())
                .memberId(bizMemberId)
                .name(form.getName())
                .description(form.getDescription())
                .extraInfo(resolveExtraInfo(form))
                .address(form.getAddress())
//                .mapx(null)
//                .mapy(null)
                .firstImage(savedUrls.get(0))
                .hashtags(normalizeHashtags(form.getHashtags()))
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
        // 읽기뷰는 목록으로, 수정 폼은 입력칸 기존값으로 쓰기 위해 "[라벨] 값" 덩어리를 미리 풀어둔다
        detail.setExtraInfoMap(BusinessExtraInfoCatalog.parse(detail.getExtraInfo()));
        return detail;
    }

    @Transactional
    public void updatePlace(Long bizMemberId, BusinessPlaceFormDto form, List<String> photoOrder,
                             List<String> removeImageUrls, List<MultipartFile> newImages) {
        requireRequiredFields(form);
        BusinessPlaceOverviewDto overview = businessMapper.selectPlaceOverviewByMember(bizMemberId);
        if (overview == null) {
            throw new IllegalStateException("등록된 업소가 없습니다.");
        }
        Long placeId = overview.getPlaceId();

        // 사진을 지우고 다시 넣기 전에 먼저 검증해야 실패 시 기존 사진이 날아가지 않는다
        PriceSetting price = resolvePrice(form);
        String extraInfo = resolveExtraInfo(form);

        List<String> currentImages = businessMapper.selectPlaceImages(placeId);
        Set<String> currentImageSet = Set.copyOf(currentImages);
        Set<String> toRemove = removeImageUrls == null ? Set.of() : Set.copyOf(removeImageUrls);

        List<String> newUrls = toNonEmpty(newImages).stream().map(this::saveImageFile).toList();

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
                .name(form.getName())
                .placeType(form.getPlaceType())
                .priceType(price.priceType())
                .minPrice(price.minPrice())
//                .regionId(form.getRegionId())
                .address(form.getAddress())
                .description(form.getDescription())
                .extraInfo(extraInfo)
                .firstImage(firstImage)
                .hashtags(normalizeHashtags(form.getHashtags()))
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

    // 업소명·주소는 폼에서 required지만 직접 요청으로 우회할 수 있어 서버에서도 확인한다
    // (DB NOT NULL 제약에 걸려 500이 나기 전에 어떤 값이 빠졌는지 알려주기 위함)
    private void requireRequiredFields(BusinessPlaceFormDto form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("업소명을 입력해주세요.");
        }
        if (form.getAddress() == null || form.getAddress().isBlank()) {
            throw new IllegalArgumentException("주소를 입력해주세요.");
        }
        if (form.getPlaceType() == null || form.getPlaceType().isBlank()) {
            throw new IllegalArgumentException("업종을 선택해주세요.");
        }
    }

    // 파일 input은 선택 없이 제출해도 빈 파트가 실려올 수 있어 걸러낸다
    private List<MultipartFile> toNonEmpty(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
    }

    /**
     * 폼에서 온 가격 입력을 DB 제약(CK_PLACE_PRICE_TYPE / CK_PLACE_MIN_PRICE)에 맞게 정리한다.
     * - 숙박이 아니면 무조건 FREE + min_price null (폼에서 가격 영역 자체가 숨겨져 값이 안 온다)
     * - FIXED면 금액 필수, 그 외에는 금액을 버린다
     * 화면 JS가 이미 같은 규칙으로 토글하지만, 폼 조작/직접 요청을 막는 서버측 최종 방어선이다.
     */
    private PriceSetting resolvePrice(BusinessPlaceFormDto form) {
        if (!PLACE_TYPE_LODGING.equals(form.getPlaceType())) {
            return new PriceSetting(PRICE_TYPE_FREE, null);
        }
        String priceType = form.getPriceType();
        if (priceType == null || !ALLOWED_PRICE_TYPES.contains(priceType)) {
            throw new IllegalArgumentException("가격 유형을 선택해주세요.");
        }
        if (!PRICE_TYPE_FIXED.equals(priceType)) {
            return new PriceSetting(priceType, null);
        }
        Integer minPrice = form.getMinPrice();
        if (minPrice == null || minPrice < 0) {
            throw new IllegalArgumentException("가격입력을 선택한 경우 금액을 0원 이상으로 입력해야 합니다.");
        }
        return new PriceSetting(PRICE_TYPE_FIXED, minPrice);
    }

    private record PriceSetting(String priceType, Integer minPrice) {}

    /**
     * 부가정보 입력칸(라벨/값 병렬 리스트)을 extra_info에 저장할 한 덩어리로 조립한다.
     * 값이 빈 항목은 빠지고, 하나도 입력하지 않았으면 null(컬럼 비움)이다.
     *
     * 라벨은 폼의 hidden input으로 오므로 조작될 수 있다. 공공데이터와 표기가 어긋난 라벨이 저장되면
     * 같은 유형의 장소가 화면에서 다르게 보이므로, 업종별 허용 라벨 목록으로 서버에서 다시 확인한다.
     * (가격 처리 resolvePrice와 같은 이유의 서버측 최종 방어선)
     */
    private String resolveExtraInfo(BusinessPlaceFormDto form) {
        return BusinessExtraInfoCatalog.assemble(
                form.getPlaceType(), form.getExtraLabels(), form.getExtraValues());
    }

    // 폼에서 콤마로 구분해 받은 해시태그를 빈 항목/앞뒤 공백 없이 정리한다.
    private String normalizeHashtags(String rawHashtags) {
        if (rawHashtags == null || rawHashtags.isBlank()) {
            return null;
        }
        String joined = Arrays.stream(rawHashtags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
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