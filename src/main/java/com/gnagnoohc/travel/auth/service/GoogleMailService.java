package com.gnagnoohc.travel.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.gnagnoohc.travel.auth.exception.EmailVerificationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleMailService {

	private final JavaMailSender mailSender;

	// 발신 주소는 코드에 직접 넣지 않고 메일 설정에서 가져온다.
	@Value("${spring.mail.username}")
	private String fromAddress;

	// 인증번호 이메일 발송
	// 설정된 발신 주소로 인증번호와 유효 시간을 안내한다.
	public void sendVerificationCode(String recipient, String verificationCode) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(recipient);
		message.setSubject("[Travel Community] 이메일 인증번호");
		message.setText(
				"이메일 인증번호는 " + verificationCode + " 입니다.\n"
				+ "인증번호는 5분 동안 유효합니다.\n"
				+ "본인이 요청하지 않았다면 이 메일을 무시해주세요."
		);

		try {
			// SMTP 오류의 상세 내용은 숨기고 사용자가 이해할 수 있는 예외로 바꾼다.
			mailSender.send(message);
		} catch (MailException e) {
			throw new EmailVerificationException("인증 메일을 발송하지 못했습니다.", e);
		}
	}
}
