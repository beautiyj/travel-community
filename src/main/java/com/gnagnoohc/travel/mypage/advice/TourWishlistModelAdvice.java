package com.gnagnoohc.travel.mypage.advice;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.mypage.service.MypageService;
import com.gnagnoohc.travel.tour.controller.TourController;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// TourController가 처리하는 요청에만 로그인 회원의 찜 상태를 모델에 주입한다.
// tour 패키지(DTO/컨트롤러)를 직접 수정하지 않고 화면에서 바로 대조할 수 있게 하기 위한 확장 지점.
@ControllerAdvice(assignableTypes = TourController.class)
@RequiredArgsConstructor
public class TourWishlistModelAdvice {

    private final MypageService mypageService;

    // tour/list.jsp에서 카드 목록을 순회하며 대조하는 용도
    @ModelAttribute("wishlistedPlaceIds")
    public Set<Integer> wishlistedPlaceIds(HttpSession session) {
        Long memberId = getMemberId(session);
        if (memberId == null) {
            return Collections.emptySet();
        }
        List<Long> ids = mypageService.getWishlistedPlaceIds(memberId);
        return ids.stream().map(Long::intValue).collect(Collectors.toSet());
    }

    // tour/detail.jsp가 이미 참조하고 있던 단건 찜 여부 (요청의 placeId 파라미터 기준)
    @ModelAttribute("isBookmarked")
    public boolean isBookmarked(
            @RequestParam(value = "placeId", required = false) Integer placeId,
            HttpSession session) {
        if (placeId == null) {
            return false;
        }
        return wishlistedPlaceIds(session).contains(placeId);
    }

    private Long getMemberId(HttpSession session) {
        Object loginMember = session.getAttribute("loginMember");
        if (loginMember instanceof LoginMemberDto member) {
            return (long) member.getMemberId();
        }
        return null;
    }
}
