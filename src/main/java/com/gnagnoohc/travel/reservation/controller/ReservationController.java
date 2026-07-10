package com.gnagnoohc.travel.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ReservationController {

    @RequestMapping("/reservation/test")
    public String reservationTest() {
        // /WEB-INF/views/reservation/test.jsp
        return "reservation/test"; 
    }
    
}
