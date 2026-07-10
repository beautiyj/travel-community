package com.gnagnoohc.travel.tour.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TourController {

    @RequestMapping("/tour/test")
    public String tourList() {
        // /WEB-INF/views/tour/test.jsp 파일과 매핑
        return "tour/test"; 
    }

    // @GetMapping("/tour/detail")
    // public String tourDetail() {
    //     // /WEB-INF/views/tour/detail.jsp 파일과 매핑
    //     return "tour/detail"; 
    // }
}