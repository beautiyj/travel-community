package com.gnagnoohc.travel;

import com.gnagnoohc.travel.tour.service.TourService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final TourService tourService;

    @GetMapping("/")
    public String index(Model model) {
        // /WEB-INF/views/main/index.jsp
        // 상위 시/도 지역 목록을 메인에도 노출하여 검색바 드롭다운에서 사용
        model.addAttribute("parentRegionList", tourService.getParentRegionList());
        return "main/index"; 
    }

    @GetMapping("/event/busan-haeundae")
    public String busanHaeundaeEvent() {
        // /WEB-INF/views/event/busan-haeundae.jsp
        return "event/busan-haeundae";
    }

}
