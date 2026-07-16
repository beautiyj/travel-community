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

    @GetMapping("/tour/list")
    public String tourList(@RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String categoryCode,
            Model model) {

        // List<TourItemDto> tourList = tourService.getTourListByArea(areaCode);
        // model.addAttribute("tourList", tourList);

        // // 지역 드롭다운용 데이터
        // List<RegionDto> regionList = regionService.getAllRegions();
        // model.addAttribute("regionList", regionList);
        // model.addAttribute("areaCode", areaCode);
        // regionList.stream().filter(r -> r.getCode().equals(areaCode)).findFirst()
        //         .ifPresent(r -> model.addAttribute("areaName", r.getName()));

        // // 카테고리 드롭다운용 데이터 (lclsSystmCode2로 미리 저장해둔 데이터)
        // List<CategoryDto> categoryList = categoryService.getAllCategories();
        // model.addAttribute("categoryList", categoryList);
        // model.addAttribute("categoryCode", categoryCode);
        // categoryList.stream().filter(c -> c.getCode().equals(categoryCode)).findFirst()
        //         .ifPresent(c -> model.addAttribute("categoryName", c.getName()));

        return "tour/list";
    }

}