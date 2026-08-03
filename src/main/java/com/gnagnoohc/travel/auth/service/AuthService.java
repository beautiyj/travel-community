package com.gnagnoohc.travel.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.gnagnoohc.travel.auth.dto.LoginMemberDto;
import com.gnagnoohc.travel.auth.dto.LocalLoginResult;
import com.gnagnoohc.travel.auth.dto.SignUpRequest;
import com.gnagnoohc.travel.auth.dto.VerifiedPasswordReset;
import com.gnagnoohc.travel.auth.dto.VerifiedSignupEmail;
import com.gnagnoohc.travel.auth.exception.EmailVerificationException;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberLocalAuth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
	private static final int MAX_FAILED_LOGIN_COUNT = 5;
	private static final long MAX_BUSINESS_REGISTRATION_FILE_SIZE = 5L * 1024 * 1024;
	private static final int MAX_FILE_NAME_CREATION_ATTEMPTS = 3;

	private final AuthMapper mapper;
	private final PasswordEncoder passEncoder;
	private final EmailVerificationService emailVerificationService;

	/*
	 * 추후 가상 저장소를 사용하면 같은 설정값을 해당 저장소의 마운트 경로로 교체한다.
	 * 생성자 기반 단위 테스트를 유지하기 위해 설정값은 생성자 주입 대상에서 제외한다.
	 */
	@Value("${file.upload-auth}")
	private String businessRegistrationUploadPath;

	// 로그인
	@Transactional
	public LocalLoginResult authenticateLocal(String username, String rawPassword) {
		if (username == null || username.isBlank()
				|| rawPassword == null || rawPassword.isBlank()) {
			return LocalLoginResult.invalidCredentials();
		}

		// 같은 아이디의 요청이 실패 횟수를 동시에 바꾸지 않도록 로그인 인증 정보를 잠가 조회한다.
		MemberLocalAuth localAuth = mapper.findLocalLoginAuthForUpdate(username.trim());
		if (localAuth == null) {
			return LocalLoginResult.invalidCredentials();
		}

		return authenticateLockedLocalAuth(localAuth, rawPassword);
	}

	/**
	 * 소셜 연동 후보 회원에게 속한 아이디만 인증한다.
	 * 다른 아이디를 제출하면 그 계정의 인증 행을 조회하지 않아 실패 횟수도 변경하지 않는다.
	 */
	@Transactional
	public LocalLoginResult authenticateSocialLinkCandidate(
			int candidateMemberId,
			String username,
			String rawPassword) {
		if (candidateMemberId <= 0
				|| username == null
				|| !username.trim().matches("^[A-Za-z0-9]{5,20}$")
				|| rawPassword == null
				|| rawPassword.isBlank()) {
			return LocalLoginResult.invalidCredentials();
		}

		MemberLocalAuth localAuth = mapper.findSocialLinkCandidateAuthForUpdate(
				candidateMemberId, username.trim());
		if (localAuth == null) {
			return LocalLoginResult.invalidCredentials();
		}

		return authenticateLockedLocalAuth(localAuth, rawPassword);
	}

	// 일반 로그인과 소셜 연동 인증이 같은 실패 횟수·잠금 정책을 사용한다.
	private LocalLoginResult authenticateLockedLocalAuth(
			MemberLocalAuth localAuth,
			String rawPassword) {
		// 잠금 중에는 비밀번호를 검사하지 않고 실패 횟수와 잠금 시간도 변경하지 않는다.
		if (localAuth.isCurrentlyLocked()) {
			return LocalLoginResult.locked();
		}

		// 만료된 잠금 정보는 다음 로그인 요청에서 초기화한다.
		if (localAuth.getLockedUntil() != null) {
			updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			localAuth.setFailedLoginCount(0);
		}

		if (passEncoder.matches(rawPassword, localAuth.getPasswordHash())) {
			// 이전에 비밀번호를 틀린 기록이 있으면 로그인 성공 시 초기화한다.
			if (localAuth.getFailedLoginCount() > 0) {
				updateOneRow(mapper.resetLoginFailure(localAuth.getUsername()));
			}
			// 비밀번호 검증까지 끝난 회원 정보만 로그인 세션 생성 대상으로 전달한다.
			return LocalLoginResult.success(new LoginMemberDto(
					localAuth.getMemberId(),
					localAuth.getNickname(),
					localAuth.getMemberType(),
					localAuth.getMemberRole()));
		}

		int nextFailedLoginCount = localAuth.getFailedLoginCount() + 1;
		if (nextFailedLoginCount >= MAX_FAILED_LOGIN_COUNT) {
			// 다섯 번째 실패에서 계정을 잠그고, 잠금 중인 요청으로 잠금 시간을 연장하지 않는다.
			updateOneRow(mapper.lockLocalLogin(localAuth.getUsername()));
			return LocalLoginResult.locked();
		}

		updateOneRow(mapper.incrementFailedLoginCount(localAuth.getUsername()));
		return LocalLoginResult.invalidCredentials();
	}

	// 인증 정보가 정확히 한 건 변경되지 않으면 트랜잭션을 롤백한다.
	private void updateOneRow(int updatedRowCount) {
		if (updatedRowCount != 1) {
			throw new IllegalStateException("로그인 인증 정보 갱신에 실패했습니다.");
		}
	}

	// 회원가입
	@Transactional
	public int memberSignUp(
			SignUpRequest signUpRequest,
			VerifiedSignupEmail sessionVerification) {

		// 입력값 검증부터 이메일 인증 결과를 회원과 연결하는 작업까지 하나의 트랜잭션으로 진행한다.
		Path storedBusinessRegistrationFile = null;
		try {
			validateSignupRequest(signUpRequest);
			String businessRegistrationExtension =
					validateBusinessRegistrationFile(signUpRequest);
			VerifiedSignupEmail verifiedEmail = emailVerificationService
					.requireVerifiedSignupEmail(signUpRequest.getEmail(), sessionVerification);

			Member member = createMember(signUpRequest, verifiedEmail);
			saveMember(member);
			saveLocalAuth(createLocalAuth(member, signUpRequest.getPassword()));

			if (signUpRequest.getMemberType() == 2) {
				storedBusinessRegistrationFile = saveBusinessRegistrationFile(
						signUpRequest.getBusinessRegistrationFile(),
						businessRegistrationExtension);
				registerFileCleanupAfterRollback(storedBusinessRegistrationFile);
				saveBusinessApplication(
						member.getMemberId(),
						storedBusinessRegistrationFile.getFileName().toString());
			}

			consumeSignupEmailVerification(verifiedEmail, member);
			return member.getMemberId();
		} catch (RuntimeException | Error e) {
			deleteStoredFile(storedBusinessRegistrationFile);
			throw e;
		}
	}

	// 회원가입 입력값 중복 확인
	public int checkLoginId(String loginId) {
		return mapper.checkLoginId(loginId);
	}

	public int checkNickname(String nickname) {
		return mapper.checkNickname(nickname);
	}

	// 회원가입 입력값 검증
	private void validateSignupRequest(SignUpRequest signUpRequest) {
		// 사용자에게 안내 가능한 회원가입 오류를 전용 예외로 전달한다.
		validateNoWhitespace("아이디", signUpRequest.getLoginId());
		validateNoWhitespace("닉네임", signUpRequest.getNickname());
		validateNoWhitespace("이름", signUpRequest.getName());
		validateNoWhitespace("비밀번호", signUpRequest.getPassword());
		validateNoWhitespace("이메일", signUpRequest.getEmail());
		validateNoWhitespace("전화번호", signUpRequest.getPhone());

		if (!signUpRequest.getPassword()
				.equals(signUpRequest.getPasswordConfirm())) {
			throw new SignupException("비밀번호가 비밀번호 확인란과 일치하지 않습니다.");
		}

		if (!signUpRequest.isPrivacyAgreed()) {
			throw new SignupException("개인정보 동의가 필요합니다.");
		}

		if (signUpRequest.getMemberType() != 1
				&& signUpRequest.getMemberType() != 2) {
			throw new SignupException("잘못된 회원 유형");
		}

		if (!isSelectableGender(signUpRequest.getGender())) {
			throw new SignupException("성별을 선택해주세요.");
		}
	}

	/**
	 * 일반 회원 요청의 파일 필드는 검증하거나 저장하지 않는다.
	 * 사업자 회원만 파일 존재 여부, 크기, 확장자와 실제 이미지 형식을 모두 검증한다.
	 */
	private String validateBusinessRegistrationFile(SignUpRequest signUpRequest) {
		if (signUpRequest.getMemberType() != 2) {
			return null;
		}

		MultipartFile file = signUpRequest.getBusinessRegistrationFile();
		if (file == null || file.isEmpty()) {
			throw new SignupException("사업자 회원은 사업자등록증 파일을 업로드해야 합니다.");
		}
		if (file.getSize() > MAX_BUSINESS_REGISTRATION_FILE_SIZE) {
			throw new SignupException("사업자등록증 파일은 5MB 이하만 업로드할 수 있습니다.");
		}

		String extension = extractAllowedImageExtension(file.getOriginalFilename());
		validateActualImageFormat(file, extension);
		return extension;
	}

	private String extractAllowedImageExtension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new SignupException("사업자등록증 파일명이 올바르지 않습니다.");
		}

		String filename = originalFilename.replace('\\', '/');
		filename = filename.substring(filename.lastIndexOf('/') + 1);
		int extensionSeparator = filename.lastIndexOf('.');
		if (extensionSeparator <= 0 || extensionSeparator == filename.length() - 1) {
			throw new SignupException("사업자등록증 파일은 JPG, JPEG, PNG 형식만 업로드할 수 있습니다.");
		}

		String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
		if (!"jpg".equals(extension)
				&& !"jpeg".equals(extension)
				&& !"png".equals(extension)) {
			throw new SignupException("사업자등록증 파일은 JPG, JPEG, PNG 형식만 업로드할 수 있습니다.");
		}
		return extension;
	}

	private void validateActualImageFormat(MultipartFile file, String extension) {
		try (InputStream fileInputStream = file.getInputStream();
				ImageInputStream imageInputStream =
						ImageIO.createImageInputStream(fileInputStream)) {
			if (imageInputStream == null) {
				throw new SignupException("유효한 JPG, JPEG, PNG 이미지 파일을 업로드해주세요.");
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
			if (!readers.hasNext()) {
				throw new SignupException("유효한 JPG, JPEG, PNG 이미지 파일을 업로드해주세요.");
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(imageInputStream, true, true);
				String actualFormat = reader.getFormatName().toUpperCase(Locale.ROOT);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width <= 0 || height <= 0
						|| !isMatchingAllowedImageFormat(extension, actualFormat)) {
					throw new SignupException("파일 확장자와 실제 이미지 형식이 일치하는 JPG, JPEG, PNG 파일을 업로드해주세요.");
				}
			} finally {
				reader.dispose();
			}
		} catch (SignupException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			throw new SignupException("유효한 JPG, JPEG, PNG 이미지 파일을 업로드해주세요.");
		}
	}

	private boolean isMatchingAllowedImageFormat(String extension, String actualFormat) {
		if ("png".equals(extension)) {
			return "PNG".equals(actualFormat);
		}
		return "JPEG".equals(actualFormat) || "JPG".equals(actualFormat);
	}

	// 회원 공통 정보 생성
	private Member createMember(SignUpRequest signUpRequest, VerifiedSignupEmail verifiedEmail) {
		// 이메일은 요청값이 아니라 DB에서 인증 완료를 확인한 값을 사용한다.
		Member member = new Member();
		member.setName(signUpRequest.getName());
		member.setLoginId(signUpRequest.getLoginId());
		member.setEmail(verifiedEmail.getEmail());
		member.setNickname(signUpRequest.getNickname());
		member.setMemberType(signUpRequest.getMemberType());
		// 입력 형식과 관계없이 같은 전화번호를 하나의 숫자 형식으로 저장한다.
		member.setPhone(signUpRequest.getPhone().replace("-", ""));
		member.setGender(toStoredGender(signUpRequest.getGender()));
		member.setBirth(signUpRequest.getBirth());
		member.setEmailVerified("Y");
		member.setEmailVerifiedAt(verifiedEmail.getVerifiedAt());
		return member;
	}

	private boolean isSelectableGender(String gender) {
		return "MALE".equals(gender)
				|| "FEMALE".equals(gender)
				|| "NONE".equals(gender);
	}

	private String toStoredGender(String gender) {
		return "NONE".equals(gender) ? null : gender;
	}

	// 회원 공통 정보 저장
	private void saveMember(Member member) {
		if (mapper.memberSignUp(member) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	// 로컬 로그인 인증 정보 생성
	private MemberLocalAuth createLocalAuth(Member member, String rawPassword) {
		MemberLocalAuth memberLocalAuth = new MemberLocalAuth();
		memberLocalAuth.setMemberId(member.getMemberId());
		memberLocalAuth.setUsername(member.getLoginId());
		memberLocalAuth.setPasswordHash(passEncoder.encode(rawPassword));
		return memberLocalAuth;
	}

	// 로컬 로그인 인증 정보 저장
	private void saveLocalAuth(MemberLocalAuth memberLocalAuth) {
		if (mapper.localMemberJoin(memberLocalAuth) != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
	}

	private Path saveBusinessRegistrationFile(MultipartFile file, String extension) {
		try {
			if (businessRegistrationUploadPath == null
					|| businessRegistrationUploadPath.isBlank()) {
				throw new IllegalStateException("사업자등록증 파일 저장 설정이 올바르지 않습니다.");
			}

			Path uploadDirectory = Paths.get(businessRegistrationUploadPath)
					.toAbsolutePath()
					.normalize();
			Files.createDirectories(uploadDirectory);

			for (int attempt = 0; attempt < MAX_FILE_NAME_CREATION_ATTEMPTS; attempt++) {
				String storedFilename = UUID.randomUUID() + "." + extension;
				Path storedFile = uploadDirectory.resolve(storedFilename).normalize();
				if (!storedFile.getParent().equals(uploadDirectory)) {
					throw new IllegalStateException("사업자등록증 파일 저장 경로가 올바르지 않습니다.");
				}

				OutputStream outputStream;
				try {
					outputStream = Files.newOutputStream(
							storedFile,
							StandardOpenOption.CREATE_NEW,
							StandardOpenOption.WRITE);
				} catch (FileAlreadyExistsException e) {
					// UUID가 충돌한 기존 파일은 절대 삭제하지 않고 새 이름으로 다시 시도한다.
					continue;
				} catch (IOException e) {
					throw new IllegalStateException("사업자등록증 파일 저장에 실패했습니다.", e);
				}

				try (outputStream;
						InputStream inputStream = file.getInputStream()) {
					inputStream.transferTo(outputStream);
					return storedFile;
				} catch (IOException e) {
					// 출력 스트림 생성 성공 이후의 실패이므로 이번 시도가 만든 파일만 삭제한다.
					deleteStoredFile(storedFile);
					throw new IllegalStateException("사업자등록증 파일 저장에 실패했습니다.", e);
				}
			}

			throw new IllegalStateException("사업자등록증 파일 이름 생성에 실패했습니다.");
		} catch (IOException e) {
			throw new IllegalStateException("사업자등록증 파일 저장에 실패했습니다.", e);
		}
	}

	private void saveBusinessApplication(int memberId, String fileKey) {
		if (mapper.insertBusinessApplication(memberId, fileKey) != 1) {
			throw new SignupException("사업자 가입 신청 저장에 실패했습니다. 다시 시도해주세요.");
		}
	}

	/**
	 * 메서드가 정상 반환된 뒤 외부 트랜잭션이 롤백되거나 커밋이 실패하는 경우에도
	 * DB에 연결되지 않은 파일이 남지 않도록 트랜잭션 최종 상태를 확인한다.
	 */
	private void registerFileCleanupAfterRollback(Path storedFile) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						if (status != TransactionSynchronization.STATUS_COMMITTED) {
							deleteStoredFile(storedFile);
						}
					}
				});
	}

	private void deleteStoredFile(Path storedFile) {
		if (storedFile == null) {
			return;
		}
		try {
			Files.deleteIfExists(storedFile);
		} catch (IOException | SecurityException ignored) {
			// 원래 예외와 롤백을 방해하지 않으며 운영에서는 고아 파일 정리 대상으로 처리한다.
			Path storedFilename = storedFile.getFileName();
			log.warn("사업자등록증 파일 삭제에 실패했습니다. storedFilename={}",
					storedFilename == null ? "unknown" : storedFilename);
		}
	}

	// 이메일 인증 결과를 회원과 연결
	private void consumeSignupEmailVerification(VerifiedSignupEmail verifiedEmail, Member member) {
		// 인증 행을 새 회원과 연결해 다른 회원가입에서 다시 사용할 수 없게 한다.
		if (mapper.consumeSignupEmailVerification(
				verifiedEmail.getEmailVerificationId(), member.getMemberId()) != 1) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이미 사용되었거나 유효하지 않은 이메일 인증입니다. 다시 인증해주세요."
			);
		}
	}

	// 입력값의 공백 포함 여부 확인
	private void validateNoWhitespace(String fieldName, String value) {
		if (value != null && value.chars().anyMatch(ch -> Character.isWhitespace(ch))) {
			throw new SignupException(fieldName + "에는 공백을 입력할 수 없습니다.");
		}
	}

	public String findId(String name, String email) {
		
		return mapper.findId(name, email);
	}

	// 비밀번호 변경과 인증 결과 소비는 반드시 함께 성공하거나 함께 롤백되어야 한다.
	@Transactional
	public void resetPassword(
			String newPassword,
			VerifiedPasswordReset sessionVerification) {
		// 세션 값만 신뢰하지 않고 최신 DB 인증 행을 잠가 만료·재발송·재사용 여부를 확인한다.
		emailVerificationService.requireVerifiedPasswordReset(sessionVerification);

		String passwordHash = passEncoder.encode(newPassword);
		// 인증 결과를 먼저 소비해 같은 이메일 인증으로 동시 변경 요청이 성공하지 못하게 한다.
		if (mapper.consumePasswordResetVerification(
				sessionVerification.getEmailVerificationId(), sessionVerification.getMemberId()) != 1) {
			throw new EmailVerificationException(
					EmailVerificationException.EMAIL_REVERIFICATION_REQUIRED,
					"이메일 인증이 만료되었거나 이미 사용되었습니다. 다시 인증해주세요.");
		}

		// 비밀번호 변경 후에는 잠금과 실패 횟수를 초기화해 새 비밀번호로 로그인할 수 있게 한다.
		if (mapper.updatePasswordByMemberId(sessionVerification.getMemberId(), passwordHash) != 1) {
			throw new IllegalStateException("비밀번호 변경에 실패했습니다.");
		}
	}

}
