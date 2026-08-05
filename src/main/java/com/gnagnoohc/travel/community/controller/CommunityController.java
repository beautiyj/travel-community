package com.gnagnoohc.travel.community.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import com.gnagnoohc.travel.community.dto.SearchTypeOption;
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

    // 검색 타입 드롭다운(제목/작성자/내용) 목록. 값이 없으면(기본 "전체") 제목+작성자 OR 검색 유지 (CommunityMapper.xml listFilter 참고)
    private static final List<SearchTypeOption> SEARCH_TYPE_OPTIONS = List.of(
            new SearchTypeOption("title", "제목"),
            new SearchTypeOption("writer", "작성자"),
            new SearchTypeOption("content", "내용")
    );

    // 목록 (페이지네이션 포함 - page는 1부터 시작)
    @GetMapping("/community/list")
    public String list(@RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "searchType", required = false) String searchType,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       Model model) {

    	model.addAllAttributes(service.selectPage(category, searchType, q, page));

        model.addAttribute("searchTypeList", SEARCH_TYPE_OPTIONS);
        // 드롭다운 선택 상태 유지용: dropdownSelector.jsp가 requestScope["searchType"]로 조회
        model.addAttribute("searchType", searchType);
        String searchTypeName = SEARCH_TYPE_OPTIONS.stream()
                .filter(option -> option.getCode().equals(searchType))
                .map(SearchTypeOption::getName)
                .findFirst()
                .orElse(null);
        model.addAttribute("searchTypeName", searchTypeName);

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

        // 일반후기는 다중 장소 태그(post_place_tag)를 별도로 조회해서 채움
        if ("일반후기".equals(post.getCategory())) {
            post.setPlaceTags(service.selectPlaceTags(postId));
        }

        model.addAttribute("post", post);

        return "community/detail";
    }


    // 글쓰기 폼 열기 (빈 화면)
    @GetMapping("/community/write")
    public String writeForm(HttpSession session, Model model) {

        // 로그인 안 한 사용자는 막기
        if (session.getAttribute("loginMember") == null) {
            return "redirect:/auth/login";
        }

        // 카테고리 드롭다운 목록: JSP에 하드코딩하지 않고 enum(PostCategory)을 그대로 넘김
        model.addAttribute("categoryList", CommunityDto.PostCategory.values());

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

        // 1-1) 장소 태그 검증: 카테고리별로 placeId/placeIds 중 허용되지 않는 쪽은 비움
        //      (화면에서 필드를 숨겨도, 폼 조작으로 넘어올 수 있으니 서버에서 한 번 더 막음)
        enforcePlaceTagRule(dto);

        // 1-2) 방문자인증후기는 장소 태그가 필수 (평소엔 클라이언트가 먼저 막지만, 폼 조작 대비 재확인)
        if (missingRequiredPlaceTag(dto)) {
            return "redirect:/community/write";
        }

        // 2) 게시글 저장 (insert 후 dto.postId 가 채워짐)
        service.insert(dto);

        // 3) 저장된 글 번호를 받아 이미지 연결 (이미지 저장 로직은 ImageService로 분리)
        int postId = dto.getPostId();

        // 3-1) 일반후기 다중 장소 태그 저장 (선택 안 했으면 그냥 건너뜀)
        if ("일반후기".equals(dto.getCategory()) && dto.getPlaceIds() != null && !dto.getPlaceIds().isEmpty()) {
            service.insertPlaceTags(postId, dto.getPlaceIds());
        }

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

        // 본문 에디터가 기존 이미지를 토큰 위치 그대로 복원해야 해서(contentEditor.js),
        // detail()과 마찬가지로 이미지 목록을 채워줘야 함 (안 그러면 전부 /upload/undefined로 깨짐)
        post.setImageList(imageService.selectImages(postId));

        // 일반후기는 다중 장소 태그를 미리 채워서 수정 폼에 pre-fill
        if ("일반후기".equals(post.getCategory())) {
            post.setPlaceTags(service.selectPlaceTags(postId));
        }

        model.addAttribute("post", post);
        // 카테고리 드롭다운 목록: JSP에 하드코딩하지 않고 enum(PostCategory)을 그대로 넘김
        model.addAttribute("categoryList", CommunityDto.PostCategory.values());

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

        // 장소 태그 검증: 카테고리별로 placeId/placeIds 중 허용되지 않는 쪽은 비움
        enforcePlaceTagRule(dto);

        // 방문자인증후기는 장소 태그가 필수 (평소엔 클라이언트가 먼저 막지만, 폼 조작 대비 재확인)
        if (missingRequiredPlaceTag(dto)) {
            return "redirect:/community/edit?postId=" + postId;
        }

        service.update(dto);                      // 제목/내용/카테고리/장소태그 수정

        // 일반후기 다중 장소 태그: 전체 교체(기존 삭제 후 재삽입)
        if ("일반후기".equals(dto.getCategory())) {
            service.deletePlaceTags(postId);
            if (dto.getPlaceIds() != null && !dto.getPlaceIds().isEmpty()) {
                service.insertPlaceTags(postId, dto.getPlaceIds());
            }
        }

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


    // 장소 검색 (글쓰기/수정 시 장소 태그 검색 모달에서 AJAX로 호출, "더보기"로 page단위 조회)
    // ※ community/comment 공통 로직이라 CommonService로 위임
    // - 방문자인증후기: 로그인 회원이 이용완료(COMPLETED)한 예약 장소만 검색
    // - 그 외(일반후기 등): 기존처럼 전체 장소 검색
    // 키워드가 빈 문자열이면 매퍼의 LIKE '%%'가 전체 행에 매칭되므로, 모달을 열어 아직
    // 검색어를 입력하지 않은 상태에서도 전체 장소 목록을 이름 가나다순으로 보여줄 수 있음
    // 응답 형태: {"items": [...], "hasMore": boolean} (page는 0부터 시작)
    @GetMapping("/community/place/search")
    @ResponseBody
    public Map<String, Object> searchPlaces(@RequestParam("keyword") String keyword,
                                             @RequestParam(value = "category", required = false) String category,
                                             @RequestParam(value = "page", defaultValue = "0") int page,
                                             HttpSession session) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();

        if ("방문자인증후기".equals(category)) {
            Object login = session.getAttribute("loginMember");
            if (login == null) {
                return Map.of("items", List.of(), "hasMore", false);
            }
            return commonService.searchConfirmedPlaces(SessionUtil.getMemberId(login), trimmedKeyword, page);
        }

        return commonService.searchPlaces(trimmedKeyword, page);
    }


    // 공통 (community 도메인 내부 전용)

    // 로그인한 사람이 글 작성자인지 확인
    private boolean isOwner(CommunityDto post, HttpSession session) {
        Object login = session.getAttribute("loginMember");
        if (login == null || post == null) return false;

        return post.getMemberId() == SessionUtil.getMemberId(login);
    }

    // 장소 태그 규칙: 방문자인증후기 = 단일 태그(placeId, 필수), 일반후기 = 다중 태그(placeIds, 선택),
    // 그 외 카테고리는 둘 다 허용 안 함. 폼 조작으로 다른 쪽 필드가 같이 넘어와도 여기서 정리함.
    private void enforcePlaceTagRule(CommunityDto dto) {
        String category = dto.getCategory();
        if ("방문자인증후기".equals(category)) {
            dto.setPlaceIds(null);
        } else if ("일반후기".equals(category)) {
            dto.setPlaceId(null);
            dto.setPlaceIds(dedupePlaceIds(dto.getPlaceIds()));
        } else {
            dto.setPlaceId(null);
            dto.setPlaceIds(null);
        }
    }

    // placeIds에서 null/중복 제거, 남는 게 없으면 null로 정규화 (foreach insert에 빈 리스트가 안 들어가게)
    private List<Integer> dedupePlaceIds(List<Integer> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return null;
        }
        List<Integer> deduped = new ArrayList<>(new LinkedHashSet<>(placeIds));
        deduped.remove(null);
        return deduped.isEmpty() ? null : deduped;
    }

    // 방문자인증후기인데 장소 태그(placeId)가 없는 경우 (필수 검증)
    private boolean missingRequiredPlaceTag(CommunityDto dto) {
        return "방문자인증후기".equals(dto.getCategory()) && dto.getPlaceId() == null;
    }

}