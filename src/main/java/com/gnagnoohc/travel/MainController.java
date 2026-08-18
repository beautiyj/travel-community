package com.gnagnoohc.travel;

import com.gnagnoohc.travel.tour.model.PlaceDTO;
import com.gnagnoohc.travel.tour.service.TourService;

import java.util.ArrayList;
import java.util.List;

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
        try {
            // 1. 상위 지역 목록 (null 방어)
            var parentRegions = tourService.getParentRegionList();
            model.addAttribute("parentRegionList", parentRegions != null ? parentRegions : new ArrayList<>());

            // 2. 각 타입별 실제 DB 장소 데이터 조회
            List<PlaceDTO> stayList = tourService.getPlaceList("stay", null, null, "latest", 1);
            List<PlaceDTO> foodList = tourService.getPlaceList("food", null, null, "latest", 1);
            List<PlaceDTO> tourList = tourService.getPlaceList("tour", null, null, "latest", 1);

            // 3. Null 방어 및 상위 4개 안전 추출
            model.addAttribute("stayList", getSafeSubList(stayList, 4));
            model.addAttribute("foodList", getSafeSubList(foodList, 4));
            model.addAttribute("tourList", getSafeSubList(tourList, 4));

        } catch (Exception e) {
            // DB 조회 중 에러가 발생해도 Whitelabel 404로 튕기지 않도록 예외 포획 후 콘솔 출력
            System.err.println("=== [MainController] 메인 데이터 조회 실패 ===");
            e.printStackTrace();
            
            // 에러 시 빈 리스트로 넘겨서 화면이 깨지지 않고 정상 렌더링되도록 처리
            model.addAttribute("parentRegionList", new ArrayList<>());
            model.addAttribute("stayList", new ArrayList<>());
            model.addAttribute("foodList", new ArrayList<>());
            model.addAttribute("tourList", new ArrayList<>());
        }

        return "main/index";
    }

    // Null 및 개수 초과 방지 헬퍼 메서드
    private List<PlaceDTO> getSafeSubList(List<PlaceDTO> list, int limit) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @GetMapping("/event/busan-haeundae")
    public String busanHaeundaeEvent() {
        return "event/busan-haeundae";
    }
}