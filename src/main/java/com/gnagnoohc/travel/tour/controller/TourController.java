package com.gnagnoohc.travel.tour.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.model.PlaceImageDTO;
import com.gnagnoohc.travel.tour.service.TourService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @RequestMapping("/tour/test")
    public String tourList() {
        return "tour/test";
    }

    // 실제 데이터베이스 연동된 장소 목록 통합 조회
    @GetMapping("/tour/list")
    public String tourList(
            @RequestParam(value = "placeType", required = false) String placeType,
            @RequestParam(value = "regionId", required = false) Integer regionId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        if (!StringUtils.hasText(placeType) || "all".equalsIgnoreCase(placeType)) {
            placeType = null;
        }

        List<PlaceDTO> placeList = tourService.getPlaceList(placeType, regionId, keyword, page);

        model.addAttribute("placeList", placeList);
        model.addAttribute("selectedPlaceType", placeType);
        model.addAttribute("selectedRegionId", regionId);
        model.addAttribute("keyword", keyword);

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
        log.info("[Detail Check] placeId: {}, placeImages 개수: {}", placeId, (placeImages != null ? placeImages.size() : "null"));

        return "tour/detail";
    }
}