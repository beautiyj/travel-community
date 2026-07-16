package com.gnagnoohc.travel.community.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.gnagnoohc.travel.community.dto.CommentDto;
import com.gnagnoohc.travel.community.dto.CommunityDto;
import com.gnagnoohc.travel.community.dto.ImageDto;
import com.gnagnoohc.travel.community.service.CommunityService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService service;
	
    // 인덱스 파일 실행
    @GetMapping("/main/index")
    public String index() {
        return "main/index";
    }
    
    // 이미지 저장
    @Value("${file.upload-community}")
    private String uploadDir;
    
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
    public String detail(@RequestParam("postId") Long postId, Model model) {

        CommunityDto post = service.selectOne(postId);
        if (post == null) {
            return "redirect:/community/list";   // 없는 글이면 목록으로
        }

        service.updateReadcount(postId);   // 글이 있을 때만 조회수 +1

        List<ImageDto> imageList = service.selectImages(postId);
        List<CommentDto> commentList = service.selectComments(postId);
        
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
            return "redirect:/member/login";
        }
        dto.setMemberId(getMemberId(login));
 
        // 2) 게시글 저장 (insert 후 dto.postId 가 채워짐)
        service.insert(dto);
 
     // 3) 저장된 글 번호를 변수로 받아 이미지 연결
        Long postId = dto.getPostId();
        saveImages(images, postId);
 
        return "redirect:/community/list";
    }
 
 
    // 수정 폼 열기 (기존 글 채워서)
    @GetMapping("/community/edit")
    public String editForm(@RequestParam("postId") Long postId, Model model, HttpSession session) {
 
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
        Long postId = dto.getPostId();
 
        service.update(dto);                 // 제목/내용/카테고리 수정
        saveImages(images, postId);          // 새 이미지가 있으면 추가
 
        return "redirect:/community/detail?postId=" + dto.getPostId();
    }
 
 
    // 삭제
    @PostMapping("/community/delete")
    public String delete(@RequestParam("postId") Long postId, HttpSession session) {
 
        CommunityDto post = service.selectOne(postId);
        if (!isOwner(post, session)) {
            return "redirect:/community/detail?postId=" + postId;
        }
 
        service.delete(postId);
        
        return "redirect:/community/list";
    }
    

    // 공통
 
    // 이미지 파일들을 디스크에 저장하고 경로를 DB(IMAGE 테이블)에 기록
    private void saveImages(MultipartFile[] images, Long postId) throws IOException {
        if (images == null) return;
 
        List<ImageDto> existingImages = service.selectImages(postId);
        int order = existingImages.size();   // 이어서 매길 시작 번호
        
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) continue;
 
            // 파일명 중복 방지: UUID + 원본이름
            String savedName = UUID.randomUUID() + "_" + image.getOriginalFilename();
 
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();       // 폴더 없으면 생성
 
            image.transferTo(new File(folder, savedName));
 
            ImageDto img = new ImageDto();
            img.setPostId(postId);        // FK 컬럼명이 post_id2
            img.setImageUrl(savedName);    // 저장된 파일명
            img.setSortOrder(order++);     // 정렬 순서
            service.insertImage(img);
        }
    }
 
    // 세션 로그인 정보에서 memberId 꺼내기
    // ※ 로그인 담당자가 세션에 무엇을 담는지에 따라 이 부분만 맞추면 됨
    private Long getMemberId(Object login) {
        // 예시 1) 세션에 회원 객체(MemberDto)를 담는 경우:
        //   return ((MemberDto) login).getMemberId();
        // 예시 2) 세션에 memberId(Long)만 담는 경우:
        //   return (Long) login;
        return (Long) login;   // ← 실제 구조에 맞춰 수정
    }
 
    // 로그인한 사람이 글 작성자인지 확인
    private boolean isOwner(CommunityDto post, HttpSession session) {
        Object login = session.getAttribute("loginMember");
        if (login == null || post == null) return false;
        
        return post.getMemberId().equals(getMemberId(login));
    }
    
}
