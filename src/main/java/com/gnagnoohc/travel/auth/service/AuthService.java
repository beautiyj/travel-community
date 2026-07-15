package com.gnagnoohc.travel.auth.service;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gnagnoohc.travel.auth.dto.SignUpRequest;
import com.gnagnoohc.travel.auth.exception.SignupException;
import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.model.Member;
import com.gnagnoohc.travel.auth.model.MemberLocalAuth;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	
	private final AuthMapper mapper;
	private final PasswordEncoder passEncoder;
	private final EmailVerificationService emailVerificationService;
	
	@Transactional
	public int memberSignUp(SignUpRequest signUpRequest) {
		
		//사업자 등록증 사진 파일 ( 추후 어드민 기능 추가 후 구현 예정)
//		String filename = mf.getOriginalFilename();
//		int size = (int)mf.getSize();
//		
//		String path = session.getServletContext().getRealPath("businessRegister");
//		int businessUpload = 0;
//		String newfilename = "";
//		
//		String extension = filename.substring(filename.lastIndexOf("."), filename.length());
//		
//		UUID uuid = UUID.randomUUID();
//		
//		newfilename = path(저장소) + uuid.toString() + extension;
//		System.out.println("newfilename:"+newfilename);
//		
//		member.setProfile_img_url(newfilename);
		
		validateNoWhitespace("아이디", signUpRequest.getLoginId());
		validateNoWhitespace("닉네임", signUpRequest.getNickname());
		validateNoWhitespace("이름", signUpRequest.getName());
		validateNoWhitespace("비밀번호", signUpRequest.getPassword());
		validateNoWhitespace("이메일", signUpRequest.getEmail());
		validateNoWhitespace("전화번호", signUpRequest.getPhone());
		
		// 사용자에게 안내 가능한 회원가입 오류를 전용 예외로 전달한다.
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

		// 회원가입 요청의 이메일은 클라이언트 상태가 아니라 DB 인증 완료 상태로 검증한다.
		String verifiedEmail = emailVerificationService
				.requireVerifiedSignupEmail(signUpRequest.getEmail());
		
		Member member = new Member();
		member.setName(signUpRequest.getName());
		member.setLogin_id(signUpRequest.getLoginId());
		member.setEmail(verifiedEmail);
		member.setNickname(signUpRequest.getNickname());
		member.setMember_type(signUpRequest.getMemberType());
		member.setPhone(signUpRequest.getPhone());
		member.setGender(signUpRequest.getGender());
		member.setBirth(signUpRequest.getBirth());
		member.setEmail_verified("Y");
		member.setEmail_verified_at(Timestamp.from(Instant.now()));
		
		int signupResult = mapper.memberSignUp(member);
		
		if(signupResult != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
		
		String encpassword = passEncoder.encode(signUpRequest.getPassword());
		
		MemberLocalAuth memberLocalAuth = new MemberLocalAuth();
		
		memberLocalAuth.setMember_id(member.getMember_id());
		memberLocalAuth.setUsername(member.getLogin_id());
		memberLocalAuth.setPassword_hash(encpassword);
		
		int localmemberresult = mapper.localMemberJoin(memberLocalAuth);
		
		if(localmemberresult != 1) {
			throw new SignupException("회원가입에 실패했습니다. 값을 다시 입력하세요.");
		}
		
		
		return member.getMember_id();
	}

	public int checkNickname(String nickname) {
		int result = mapper.checkNickname(nickname);
		
		return result;
	}

	public int checkLoginId(String loginId) {
		int result = mapper.checkLoginId(loginId);

		return result;
	}

	private void validateNoWhitespace(String fieldName, String value) {
		if (value != null && value.chars().anyMatch(ch -> Character.isWhitespace(ch))) {
			throw new SignupException(fieldName + "에는 공백을 입력할 수 없습니다.");
		}
	}

		

}
