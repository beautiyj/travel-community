package com.gnagnoohc.travel.tour.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TourController {

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

    // TODO: 미완성, 테스트용로직연결만 생성. 이후 수정필요. 필요 폼: 전체데이터목록창/클릭시넘어가는상세창 2개만.
    @GetMapping("tour/list")
    public String tourList(
            @RequestParam(value = "placeType", defaultValue = "tour") String placeType,
            @RequestParam(value = "regionId", required = false) Integer regionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        // 1. 조건별 및 가상 비즈니스 계정 상위 노출 적용된 장소 목록 조회
//        List<PlaceDTO> placeList = tourService.getPlaceList(placeType, regionId, page);

        // 2. 뷰(JSP) 단으로 데이터 넘기기
//        model.addAttribute("placeList", placeList);
        model.addAttribute("selectedPlaceType", placeType); // stay, food, tour
        model.addAttribute("selectedRegionId", regionId);

        return "tour/list"; // -> 하나의 통합 뷰 파일(/WEB-INF/views/tour/list.jsp)로 리턴!
    }

}