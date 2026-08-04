package com.gnagnoohc.travel.admin.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gnagnoohc.travel.admin.service.AdminBusinessApplicationService;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 사업자 신청 목록·상세·증빙 열람·심사 요청의 HTTP 계층이다.
 * <p>
 * 세션의 관리자 형식을 먼저 확인하고 서비스에 회원 ID를 전달한다. 세션은 오래 남을 수 있으므로
 * 최종 인가는 서비스가 현재 DB 상태로 다시 확인한다. 승인/반려 규칙과 동시성 제어는 서비스에
 * 두고, 이 클래스는 요청 파싱, 뷰 선택과 HTTP 오류 변환만 담당한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/business-applications")
public class AdminBusinessApplicationController {

	private final AdminBusinessApplicationService service;

	@GetMapping
	public String list(
			@RequestParam(value = "status", required = false) String status,
			HttpServletRequest request,
			HttpServletResponse response,
			Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		String selectedStatus = resolveApplicationStatus(status);
		preventCachedAdminPage(response);
		model.addAttribute(
				"applications",
				service.getApplications(admin.getMemberId(), selectedStatus));
		model.addAttribute("selectedStatus", selectedStatus);
		return "admin/business-application-list";
	}

	@GetMapping("/{applicationId}")
	public String detail(
			@PathVariable int applicationId,
			@RequestParam(value = "status", required = false) String status,
			HttpServletRequest request,
			HttpServletResponse response,
			Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		String selectedStatus = resolveApplicationStatus(status);
		preventCachedAdminPage(response);
		addApplication(model, applicationId, admin.getMemberId());
		model.addAttribute("selectedStatus", selectedStatus);
		return "admin/business-application-detail";
	}

	/**
	 * 서버가 DB에서 찾은 파일 키만 사용해 증빙 이미지를 응답한다.
	 * 경로 검증은 서비스가 담당하며, 이 응답은 캐시와 MIME 추측을 막아 관리자 전용 문서가
	 * 브라우저 캐시나 잘못된 콘텐츠 해석으로 노출되는 범위를 줄인다.
	 */
	@GetMapping("/{applicationId}/document")
	public ResponseEntity<Resource> document(
			@PathVariable int applicationId,
			HttpServletRequest request) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		Path document = service.getBusinessRegistrationDocument(
				applicationId, admin.getMemberId());

		MediaType mediaType = determineMediaType(document);
		String extension = getSafeExtension(document);
		String downloadName = "business-registration-" + applicationId + extension;

		return ResponseEntity.ok()
				.contentType(mediaType)
				.cacheControl(CacheControl.noStore())
				.header("X-Content-Type-Options", "nosniff")
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline()
								.filename(downloadName)
								.build()
								.toString())
				.body(new FileSystemResource(document));
	}

	/**
	 * 승인 유스케이스를 실행한 뒤 PRG(Post/Redirect/Get) 방식으로 상세 화면에 돌아간다.
	 * 새로고침이 같은 POST를 다시 보내는 것을 막지만, 동시 요청의 최종 방어는 DB 행 잠금이다.
	 */
	@PostMapping("/{applicationId}/approve")
	public String approve(
			@PathVariable int applicationId,
			@RequestParam(value = "status", required = false) String status,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		String selectedStatus = resolveApplicationStatus(status);
		try {
			service.approve(applicationId, admin.getMemberId());
			return "redirect:/admin/business-applications/"
					+ applicationId + "?reviewed=approved&status="
					+ selectedStatus;
		} catch (IllegalStateException e) {
			// POST 오류도 GET으로 되돌려 브라우저의 POST 재전송을 막는다.
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return detailRedirectUrl(applicationId, selectedStatus);
		}
	}

	@PostMapping("/{applicationId}/reject")
	public String reject(
			@PathVariable int applicationId,
			@RequestParam(value = "reason", required = false) String reason,
			@RequestParam(value = "status", required = false) String status,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		String selectedStatus = resolveApplicationStatus(status);
		try {
			service.reject(applicationId, admin.getMemberId(), reason);
			return "redirect:/admin/business-applications/"
					+ applicationId + "?reviewed=rejected&status="
					+ selectedStatus;
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			redirectAttributes.addFlashAttribute("rejectionReasonInput", reason);
			return detailRedirectUrl(applicationId, selectedStatus);
		}
	}

	private void addApplication(
			Model model,
			int applicationId,
			int adminMemberId) {
		model.addAttribute(
				"application",
				service.getApplication(applicationId, adminMemberId));
	}

	private String detailRedirectUrl(int applicationId, String selectedStatus) {
		return "redirect:/admin/business-applications/" + applicationId
				+ "?status=" + selectedStatus;
	}

	/**
	 * 외부 입력은 화면에서 허용한 네 상태값만 받는다. 예상하지 않은 값이
	 * 조회 조건으로 전달되는 일을 막기 위해 잘못된 값은 400으로 처리한다.
	 */
	private String resolveApplicationStatus(String status) {
		if (status == null) {
			return "ALL";
		}
		if ("ALL".equals(status)
				|| "PENDING".equals(status)
				|| "REJECTED".equals(status)
				|| "APPROVED".equals(status)) {
			return status;
		}
		throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "허용하지 않는 신청 상태입니다.");
	}

	/**
	 * 다른 관리자의 심사 결과가 반영된 뒤 뒤로가기로 오래된 목록·상세 화면이
	 * 복원되지 않도록 동적 관리자 화면 응답을 저장하지 않는다.
	 */
	private void preventCachedAdminPage(HttpServletResponse response) {
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");
		response.setDateHeader(HttpHeaders.EXPIRES, 0);
	}

	private MediaType determineMediaType(Path document) {
		try {
			String detectedType = Files.probeContentType(document);
			if (detectedType != null) {
				MediaType mediaType = MediaType.parseMediaType(detectedType);
				if (MediaType.IMAGE_PNG.includes(mediaType)
						|| MediaType.IMAGE_JPEG.includes(mediaType)) {
					return mediaType;
				}
			}
		} catch (IOException | IllegalArgumentException ignored) {
			// 가입 시 PNG/JPEG만 저장하지만 운영체제의 MIME 감지가 실패할 수 있어 확장자를 한 번 더 확인한다.
		}

		String filename = document.getFileName().toString().toLowerCase();
		if (filename.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG;
		}
		throw new ResponseStatusException(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"지원하지 않는 사업자등록증 파일 형식입니다.");
	}

	private String getSafeExtension(Path document) {
		String filename = document.getFileName().toString().toLowerCase();
		if (filename.endsWith(".png")) {
			return ".png";
		}
		if (filename.endsWith(".jpeg")) {
			return ".jpeg";
		}
		return ".jpg";
	}

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<Void> handleDatabaseAuthorizationFailure() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Void> handleNotFound() {
		return ResponseEntity.notFound().build();
	}
}
