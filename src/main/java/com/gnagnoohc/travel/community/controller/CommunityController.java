package com.gnagnoohc.travel.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CommunityController {
    
    @RequestMapping("/community/test")
    public String communityTest() {
        // /WEB-INF/views/community/test.jsp
        return "community/test"; 
    }
    
}
