package com.gnagnoohc.travel.mypage.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gnagnoohc.travel.mypage.dto.MypageDto;
import com.gnagnoohc.travel.mypage.service.MypageService;

@Controller
@RequestMapping("/mypage")
public class MypageController {
	
	@Autowired
	private MypageService mypageService;

    @GetMapping("/test")
    public String mypageTest() {
        // /WEB-INF/views/mypage/test.jsp
        return "mypage/test"; 
    }
    
    @GetMapping("")
    public String mypage() {
    	return "mypage/mypage";
    }
    
    @GetMapping("/info")
    public String memberInfo(Model model) {
    	
    	MypageDto member = mypageService.getMemberInfo(1L);
    	System.out.println("member:"+ member);
    	
    	model.addAttribute("member", member);
    	
    	return "mypage/info";
    }
    
    @GetMapping("/edit")
    public String editForm(Model model) {
    	
    	MypageDto member = mypageService.getMemberInfo(1L);
    	System.out.println("member:" + member);
    	
    	model.addAttribute("member", member);
    	
    	return "mypage/edit";
    }
    
    @PostMapping("/edit")
    public String editMember(MypageDto member) {
    	System.out.println("수정할 member:" + member);
    	
    	mypageService.updateMember(member);
    	
    	return "redirect:/mypage/info";
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
    	
    	MypageDto member = mypageService.getMemberInfo(1L);
    	
    	System.out.println("password member:" + member);
    	
    	model.addAttribute("member", member);
    	
    	return "mypage/password";
    }
    
    /*
    @PostMapping("/password")
    public String changePassword(MypageDto member, Model model) {

        System.out.println("password change member:" + member);

        if (!member.getNewPassword().equals(member.getNewPasswordCheck())) {
            model.addAttribute("member", member);
            return "mypage/password";
        }

        mypageService.changePassword(member);

        return "redirect:/mypage/info";
    }
    */
    
    @GetMapping("/reservation")
    public String reservation(Model model) {
    	
    	List<MypageDto> reservationList = mypageService.getReservationList(1L);
    	
    	System.out.println("reservationList:" + reservationList);
    	
    	model.addAttribute("reservationList", reservationList);
    	
    	return "mypage/reservation";
    }
        
    @GetMapping("/wishlist")
    public String wishlist(Model model) {
    	
    	List<MypageDto> wishlist = mypageService.getWishlist(1L);
    	
    	System.out.println("wishlist:" + wishlist);
    	
    	model.addAttribute("wishlist", wishlist);
    	
    	return "mypage/wishlist";
    }
      
    @GetMapping("/withdraw")
    public String withdraw() {
    	return "mypage/withdraw";
    }
}
