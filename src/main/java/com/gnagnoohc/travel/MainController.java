package com.gnagnoohc.travel;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String index() {
        // /WEB-INF/views/main/index.jsp
        return "main/index";
    }

    @GetMapping("/event/busan-haeundae")
    public String busanHaeundaeEvent() {
        // /WEB-INF/views/event/busan-haeundae.jsp
        return "event/busan-haeundae";
    }

}
