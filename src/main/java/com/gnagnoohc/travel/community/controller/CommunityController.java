package com.gnagnoohc.travel.community.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.ImageDto;
import com.gnagnoohc.travel.community.service.CommentService;
import com.gnagnoohc.travel.community.service.CommonService;
import com.gnagnoohc.travel.community.service.CommunityService;
import com.gnagnoohc.travel.community.service.ImageService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService service;
    private final CommentService commentService;   // 상세 화면 댓글 목록 표시용
    private final ImageService imageService;        // 이미지 저장/조회 (image 부분 분리)
    private final CommonService commonService;      // 장소 태그 검색 (community/comment 공통)

    // 커뮤니티 테스트 파일 실행
    @GetMapping("/community/test")
    public String index() {
        return "community/test";
    }

    // 목록
    @GetMapping("/community/list")
    public String list(@RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "q", required = false) String q,
                       Model model) {

    	List<CommunityDto> postList = service.selectAll(category, q);
    	model.addAttribute("postList", postList);

        return "community/list";
    }

    // 상세 (조회수 증가 포함)
    @GetMapping("/community/detail")
    public String detail(@RequestParam("postId") int postId, Model model) {

        CommunityDto post = service.selectOne(postId);
        if (post == null) {
            return "redirect:/community/list";   // 없는 글이면 목록으로
        }

        service.updateReadcount(postId);   // 글이 있을 때만 조회수 +1

        List<ImageDto> imageList = imageService.selectImages(postId);
        List<CommentDto> commentList = commentService.selectComments(postId);

        post.setImageList(imageList);
        post.setCommentList(commentList);

        model.addAttribute("post", post);

        return "community/detail";
    }


    // 글쓰기 폼 열기 (빈 화면)
    @GetMapping("/community/write")
    public String writeForm(HttpSession session) {

        // 로그인 안 한 사용자는 막기
        if (session.getAttribute("loginMember") == null) {
            return "redirect:/member/login";
        }

        return "community/write";
    }


    // 글쓰기 처리 (등록)
    @PostMapping("/community/write")
    public String write(CommunityDto dto,
                        @RequestParam(value = "images", required = false) MultipartFile[] images,
                        HttpSession session) throws IOException {

    	// 1) 로그인 회원 확인 → memberId 세팅
        //    ※ "loginMember" key와 타입은 로그인 담당자와 맞춰야 함
        //    nickname은 저장 안 함 (조회 시 member JOIN으로 가져옴)
        Object login = session.getAttribute("loginMember");
        if (login == null) {
            return "redirect:/auth/login";
        }
        dto.setMemberId(SessionUtil.getMemberId(login));

        // 1-1) 장소 태그 검증: 방문자인증후기 카테고리가 아니면 placeId 무시
        //      (화면에서 필드를 숨겨도, 폼 조작으로 placeId가 넘어올 수 있으니 서버에서 한 번 더 막음)
        enforcePlaceTagRule(dto);

        // 2) 게시글 저장 (insert 후 dto.postId 가 채워짐)
        service.insert(dto);

        // 3) 저장된 글 번호를 받아 이미지 연결 (이미지 저장 로직은 ImageService로 분리)
        int postId = dto.getPostId();
        imageService.saveImages(images, postId);

        return "redirect:/community/list";
    }


    // 수정 폼 열기 (기존 글 채워서)
    @GetMapping("/community/edit")
    public String editForm(@RequestParam("postId") int postId, Model model, HttpSession session) {

        CommunityDto post = service.selectOne(postId);

        // 본인 글이 아니면 막기
        if (!isOwner(post, session)) {
            return "redirect:/community/detail?postId=" + postId;
        }

        model.addAttribute("post", post);

        return "community/edit";
    }


    // 수정 처리
    @PostMapping("/community/update")
    public String update(CommunityDto dto,
                         @RequestParam(value = "images", required = false) MultipartFile[] images,
                         HttpSession session) throws IOException {

        // 수정 전 원본으로 소유자 검증
        CommunityDto origin = service.selectOne(dto.getPostId());
        if (!isOwner(origin, session)) {
            return "redirect:/community/detail?postId=" + dto.getPostId();
        }
        int postId = dto.getPostId();

        // 장소 태그 검증: 방문자인증후기 카테고리가 아니면 placeId 무시
        enforcePlaceTagRule(dto);

        service.update(dto);                      // 제목/내용/카테고리/장소태그 수정
        imageService.saveImages(images, postId);   // 새 이미지가 있으면 추가 (image 부분 분리)

        return "redirect:/community/detail?postId=" + dto.getPostId();
    }


    // 삭제
    @PostMapping("/community/delete")
    public String delete(@RequestParam("postId") int postId, HttpSession session) {

        CommunityDto post = service.selectOne(postId);
        if (!isOwner(post, session)) {
            return "redirect:/community/detail?postId=" + postId;
        }

        service.delete(postId);

        return "redirect:/community/list";
    }


    // 장소 검색 (글쓰기/수정 시 장소 태그 검색 모달에서 AJAX로 호출)
    // ※ community/comment 공통 로직이라 CommonService로 위임
    // - 방문자인증후기: 로그인 회원이 확정(결제완료)한 예약 장소만 검색
    // - 그 외(일반후기 등): 기존처럼 전체 장소 검색
    @GetMapping("/community/place/search")
    @ResponseBody
    public List<Map<String, Object>> searchPlaces(@RequestParam("keyword") String keyword,
                                                   @RequestParam(value = "category", required = false) String category,
                                                   HttpSession session) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        if ("방문자인증후기".equals(category)) {
            Object login = session.getAttribute("loginMember");
            if (login == null) {
                return List.of();
            }
            return commonService.searchConfirmedPlaces(SessionUtil.getMemberId(login), keyword.trim());
        }

        return commonService.searchPlaces(keyword.trim());
    }


    // 공통 (community 도메인 내부 전용)

    // 로그인한 사람이 글 작성자인지 확인
    private boolean isOwner(CommunityDto post, HttpSession session) {
        Object login = session.getAttribute("loginMember");
        if (login == null || post == null) return false;

        return post.getMemberId() == SessionUtil.getMemberId(login);
    }

    // 장소 태그는 "방문자인증후기"/"일반후기" 카테고리에서만 허용 (1게시글 1장소)
    // 다른 카테고리인데 placeId가 넘어왔다면(폼 조작 등) 무시하고 null 처리
    private void enforcePlaceTagRule(CommunityDto dto) {
        String category = dto.getCategory();
        boolean placeTagAllowed = "방문자인증후기".equals(category) || "일반후기".equals(category);
        if (!placeTagAllowed) {
            dto.setPlaceId(null);
        }
    }

}