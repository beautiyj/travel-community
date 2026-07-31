package com.gnagnoohc.travel.tour.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.service.TourService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;
    
    @RequestMapping("/tour/test")
    public String tourList() {
        // /WEB-INF/views/tour/test.jsp 파일과 매핑
        return "tour/test";
    }

    // @GetMapping("/tour/detail")
    // public String tourDetail() {
    // // /WEB-INF/views/tour/detail.jsp 파일과 매핑
    // return "tour/detail";
    // }

    // 실제 데이터베이스 연동된 장소 목록 통합 조회 (tour, stay, food 공용)
    @GetMapping("/tour/list")
    public String tourList(
            @RequestParam(value = "placeType", defaultValue = "tour") String placeType,
            @RequestParam(value = "regionId", required = false) Integer regionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        // 1. 서비스 및 매퍼를 통해 실제 DB에서 조건별 장소 목록 조회
        List<PlaceDTO> placeList = tourService.getPlaceList(placeType, regionId, page);
        
        // 2. 뷰(JSP) 단으로 데이터 및 필터 상태 전달
        model.addAttribute("placeList", placeList);
        model.addAttribute("selectedPlaceType", placeType); // stay, food, tour
        model.addAttribute("selectedRegionId", regionId);

        return "tour/list"; // 통합 뷰 파일 (/WEB-INF/views/tour/list.jsp)
    }

}