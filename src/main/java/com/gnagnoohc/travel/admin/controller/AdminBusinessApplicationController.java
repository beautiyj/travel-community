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

import com.gnagnoohc.travel.admin.service.AdminBusinessApplicationService;
import com.gnagnoohc.travel.auth.dto.LoginMemberDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/business-applications")
public class AdminBusinessApplicationController {

	private final AdminBusinessApplicationService service;

	@GetMapping
	public String list(HttpServletRequest request, Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		model.addAttribute(
				"applications",
				service.getApplications(admin.getMemberId()));
		return "admin/business-application-list";
	}

	@GetMapping("/{applicationId}")
	public String detail(
			@PathVariable int applicationId,
			HttpServletRequest request,
			Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		addApplication(model, applicationId, admin.getMemberId());
		return "admin/business-application-detail";
	}

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

	@PostMapping("/{applicationId}/approve")
	public String approve(
			@PathVariable int applicationId,
			HttpServletRequest request,
			Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		try {
			service.approve(applicationId, admin.getMemberId());
			return "redirect:/admin/business-applications/"
					+ applicationId + "?reviewed=approved";
		} catch (IllegalStateException e) {
			addApplication(model, applicationId, admin.getMemberId());
			model.addAttribute("errorMessage", e.getMessage());
			return "admin/business-application-detail";
		}
	}

	@PostMapping("/{applicationId}/reject")
	public String reject(
			@PathVariable int applicationId,
			@RequestParam(value = "reason", required = false) String reason,
			HttpServletRequest request,
			Model model) {
		LoginMemberDto admin = AdminSessionSupport.requireAdmin(request);
		try {
			service.reject(applicationId, admin.getMemberId(), reason);
			return "redirect:/admin/business-applications/"
					+ applicationId + "?reviewed=rejected";
		} catch (IllegalArgumentException | IllegalStateException e) {
			addApplication(model, applicationId, admin.getMemberId());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("rejectionReasonInput", reason);
			return "admin/business-application-detail";
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
			// 가입 시 PNG/JPEG만 저장하지만 운영체제의 MIME 감지가 실패할 수 있어 확장자를 한 번 더 확인합니다.
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
