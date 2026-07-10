package com.gnagnoohc.travel.mypage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MypageController {

    @RequestMapping("/mypage/test")
    public String mypageTest() {
        // /WEB-INF/views/mypage/test.jsp
        return "mypage/test"; 
    }
}
