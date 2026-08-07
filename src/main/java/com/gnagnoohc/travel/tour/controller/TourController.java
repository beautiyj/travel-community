package com.gnagnoohc.travel.tour.controller;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.model.RegionDTO;
import com.gnagnoohc.travel.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    // 실제 데이터베이스 연동된 장소 목록 통합 조회
    @GetMapping("/tour/list")
    public String tourList(
            @RequestParam(value = "placeType", required = false) String placeType,
            @RequestParam(value = "regionId", required = false) Integer regionId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        if (!StringUtils.hasText(placeType) || "all".equalsIgnoreCase(placeType)) {
            placeType = null;
        }

        // 목록 + 페이징 메타데이터 일괄 수집
        Map<String, Object> pageData = tourService.getPlacePage(placeType, regionId, keyword, sort, page);

        List<RegionDTO> parentRegionList = tourService.getParentRegionList();

        String selectedRegionName = null;
        if (regionId != null && parentRegionList != null) {
            for (RegionDTO reg : parentRegionList) {
                // RegionDTO의 PK(regionId) 비교
                if (reg.getRegionId() != null && reg.getRegionId().equals(regionId)) {
                    // shortName이 있으면 shortName 사용, 없으면 regionName 사용
                    selectedRegionName = StringUtils.hasText(reg.getShortName()) ? reg.getShortName() : reg.getRegionName();
                    break;
                }
            }
        }

        model.addAllAttributes(pageData);    
        model.addAttribute("selectedPlaceType", placeType);
        model.addAttribute("selectedRegionId", regionId);
        model.addAttribute("selectedRegionName", selectedRegionName);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("parentRegionList", parentRegionList);

        return "tour/list";
    }
    
    // 실제 데이터베이스 연동된 장소 상세 조회 (placeId: Integer로 수정)
    @GetMapping("/tour/detail")
    public String tourDetail(@RequestParam("placeId") Integer placeId, Model model) {
        PlaceDTO place = tourService.getPlaceDetail(placeId);
        List<PlaceImageDTO> placeImages = tourService.getPlaceImages(placeId);

        List<String> extraInfoLines = (place != null)
                ? tourService.getExtraInfoLines(place.getExtraInfo())
                : List.of();

        model.addAttribute("place", place);
        model.addAttribute("placeImages", placeImages);
        model.addAttribute("extraInfoLines", extraInfoLines);

        return "tour/detail";
    }
}