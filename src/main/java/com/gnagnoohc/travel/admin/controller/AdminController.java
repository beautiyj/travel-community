package com.gnagnoohc.travel.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @RequestMapping("/admin/test")
    public String adminTest() {
        // /WEB-INF/views/admin/test.jsp
        return "admin/test";
    }

    // TODO: 로직 작성 후 mapper/service 연동으로 교체 예정. 지금은 화면 확인용 더미 데이터.
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("bizName", "제주 신라호텔");
        model.addAttribute("ownerName", "홍길동");
        model.addAttribute("isClosed", false);
        model.addAttribute("todayLabel", "2026년 7월 13일 (월)");

        List<Map<String, Object>> reservations = new ArrayList<>();
        reservations.add(reservation(1, "박민준", "010-2345-6789", 2, 280000, "대기중"));
        reservations.add(reservation(2, "이수연", "010-3456-7890", 4, 560000, "확정"));
        reservations.add(reservation(3, "한소희", "010-7890-1234", 2, 280000, "대기중"));
        model.addAttribute("todayReservations", reservations);

        long pendingCount = reservations.stream().filter(r -> "대기중".equals(r.get("status"))).count();
        int todayVisitors = reservations.stream()
                .filter(r -> "확정".equals(r.get("status")))
                .mapToInt(r -> (int) r.get("people"))
                .sum();

        model.addAttribute("monthlyCount", 38);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("todayVisitors", todayVisitors);
        model.addAttribute("cancelRequestCount", 1); // 예약관리 탭 사이드바 배지용 더미

        return "admin/dashboard";
    }

    private Map<String, Object> reservation(int id, String guestName, String phone, int people, int price, String status) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", id);
        r.put("guestName", guestName);
        r.put("phone", phone);
        r.put("people", people);
        r.put("price", price);
        r.put("status", status);
        return r;
    }
}