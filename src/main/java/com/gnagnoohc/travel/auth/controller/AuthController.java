package com.gnagnoohc.travel.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthController {
    
    @RequestMapping("/auth/test")
    public String authTest() {
        // /WEB-INF/views/auth/test.jsp
        return "auth/test"; 
    }
}
