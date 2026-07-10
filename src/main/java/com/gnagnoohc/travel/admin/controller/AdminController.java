package com.gnagnoohc.travel.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AdminController {

    @RequestMapping("/admin/test")
    public String adminTest() {
        // /WEB-INF/views/admin/test.jsp
        return "admin/test"; 
    }
}