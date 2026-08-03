package com.gnagnoohc.travel.admin.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.admin.dto.AdminBusinessApplicationDto;
import com.gnagnoohc.travel.admin.dto.AdminDashboardSummaryDto;
import com.gnagnoohc.travel.admin.mapper.AdminBusinessApplicationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminBusinessApplicationService {

	private static final int MAX_REJECTION_REASON_LENGTH = 500;

	private final AdminBusinessApplicationMapper mapper;

	@Value("${file.upload-auth}")
	private String businessRegistrationUploadPath;

	@Transactional(readOnly = true)
	public AdminDashboardSummaryDto getDashboardSummary(int adminMemberId) {
		requireActiveAdmin(adminMemberId);
		AdminDashboardSummaryDto summary = mapper.findDashboardSummary();
		// 집계 쿼리는 항상 한 행을 반환하지만, 예외적인 null 매핑에도 화면이 깨지지 않게 0건 DTO를 반환합니다.
		return summary == null ? new AdminDashboardSummaryDto() : summary;
	}

	@Transactional(readOnly = true)
	public List<AdminBusinessApplicationDto> getApplications(int adminMemberId) {
		requireActiveAdmin(adminMemberId);
		return mapper.findAllApplications();
	}

	@Transactional(readOnly = true)
	public AdminBusinessApplicationDto getApplication(
			int applicationId,
			int adminMemberId) {
		requireActiveAdmin(adminMemberId);
		return requireApplication(applicationId);
	}

	/**
	 * DB에 저장된 파일 키만 사용해 등록증 경로를 찾습니다.
	 * 요청에서 파일 경로를 직접 받지 않아 다른 서버 파일을 열람하는 것을 막습니다.
	 */
	@Transactional(readOnly = true)
	public Path getBusinessRegistrationDocument(
			int applicationId,
			int adminMemberId) {
		requireActiveAdmin(adminMemberId);
		validateApplicationId(applicationId);

		String fileKey = mapper.findBusinessRegistrationFileKey(applicationId);
		if (fileKey == null || fileKey.isBlank()) {
			throw new NoSuchElementException("사업자등록증 파일을 찾을 수 없습니다.");
		}

		return resolveStoredDocument(fileKey);
	}

	/**
	 * 신청 승인과 회원 권한 변경은 하나의 트랜잭션에서 처리합니다.
	 * 둘 중 하나라도 정확히 한 행을 변경하지 못하면 예외로 전체 작업을 롤백합니다.
	 */
	@Transactional
	public void approve(int applicationId, int adminMemberId) {
		requireActiveAdmin(adminMemberId);
		AdminBusinessApplicationDto application = lockPendingApplication(applicationId);

		if (mapper.approveApplication(applicationId, adminMemberId) != 1) {
			throw new IllegalStateException("이미 처리되었거나 승인할 수 없는 신청입니다.");
		}

		if (mapper.grantBusinessRole(application.getMemberId()) != 1) {
			throw new IllegalStateException("활성 상태의 승인 대기 사업자 회원만 승인할 수 있습니다.");
		}
	}

	@Transactional
	public void reject(
			int applicationId,
			int adminMemberId,
			String rejectionReason) {
		String normalizedReason = normalizeRejectionReason(rejectionReason);
		requireActiveAdmin(adminMemberId);
		lockPendingApplication(applicationId);

		if (mapper.rejectApplication(
				applicationId, adminMemberId, normalizedReason) != 1) {
			throw new IllegalStateException("이미 처리되었거나 반려할 수 없는 신청입니다.");
		}
	}

	private void requireActiveAdmin(int adminMemberId) {
		if (adminMemberId <= 0 || mapper.countActiveAdmin(adminMemberId) != 1) {
			// 세션 값만으로 관리 권한을 확정하지 않고 매 요청마다 현재 DB 상태를 확인합니다.
			throw new SecurityException("현재 사용자는 관리자 기능을 사용할 수 없습니다.");
		}
	}

	private AdminBusinessApplicationDto requireApplication(int applicationId) {
		validateApplicationId(applicationId);
		AdminBusinessApplicationDto application =
				mapper.findApplicationById(applicationId);
		if (application == null) {
			throw new NoSuchElementException("사업자 인증 신청을 찾을 수 없습니다.");
		}
		return application;
	}

	private AdminBusinessApplicationDto lockPendingApplication(int applicationId) {
		validateApplicationId(applicationId);
		AdminBusinessApplicationDto application =
				mapper.findReviewTargetForUpdate(applicationId);
		if (application == null) {
			throw new NoSuchElementException("사업자 인증 신청을 찾을 수 없습니다.");
		}
		if (!"PENDING".equals(application.getApplicationStatus())) {
			throw new IllegalStateException("이미 처리된 사업자 인증 신청입니다.");
		}
		return application;
	}

	private String normalizeRejectionReason(String rejectionReason) {
		String normalized =
				rejectionReason == null ? "" : rejectionReason.strip();
		int characterCount = normalized.codePointCount(0, normalized.length());
		if (characterCount < 1 || characterCount > MAX_REJECTION_REASON_LENGTH) {
			throw new IllegalArgumentException("반려 사유를 1자 이상 500자 이내로 입력해 주세요.");
		}
		return normalized;
	}

	private Path resolveStoredDocument(String fileKey) {
		if (businessRegistrationUploadPath == null
				|| businessRegistrationUploadPath.isBlank()) {
			throw new IllegalStateException("사업자등록증 저장 경로가 설정되지 않았습니다.");
		}

		Path keyPath;
		try {
			keyPath = Paths.get(fileKey);
		} catch (RuntimeException e) {
			throw new NoSuchElementException("사업자등록증 파일 키가 올바르지 않습니다.", e);
		}
		if (keyPath.isAbsolute() || keyPath.getNameCount() != 1) {
			throw new NoSuchElementException("사업자등록증 파일 키가 올바르지 않습니다.");
		}

		Path uploadDirectory = Paths.get(businessRegistrationUploadPath)
				.toAbsolutePath()
				.normalize();
		Path storedFile = uploadDirectory.resolve(keyPath).normalize();
		if (!storedFile.startsWith(uploadDirectory)
				|| !uploadDirectory.equals(storedFile.getParent())) {
			throw new NoSuchElementException("사업자등록증 파일 경로가 올바르지 않습니다.");
		}

		try {
			Path realUploadDirectory = uploadDirectory.toRealPath();
			Path realStoredFile = storedFile.toRealPath();
			// 심볼릭 링크가 저장 디렉터리 밖을 가리키는 경우도 열람하지 않습니다.
			if (!realStoredFile.startsWith(realUploadDirectory)
					|| !realUploadDirectory.equals(realStoredFile.getParent())
					|| !Files.isRegularFile(realStoredFile)
					|| !Files.isReadable(realStoredFile)) {
				throw new NoSuchElementException("사업자등록증 파일을 찾을 수 없습니다.");
			}
			return realStoredFile;
		} catch (IOException | SecurityException e) {
			throw new NoSuchElementException("사업자등록증 파일을 찾을 수 없습니다.", e);
		}
	}

	private void validateApplicationId(int applicationId) {
		if (applicationId <= 0) {
			throw new NoSuchElementException("사업자 인증 신청을 찾을 수 없습니다.");
		}
	}
}
