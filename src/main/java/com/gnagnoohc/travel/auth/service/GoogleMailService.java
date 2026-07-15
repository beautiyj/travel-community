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

	// Google SMTP 계정은 코드에 직접 넣지 않고 spring.mail.username으로 주입한다.
	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromAddress;

	// 인증번호 메일의 제목과 본문을 만들고 SMTP 서버로 발송한다.
	// 발신 주소는 클라이언트 입력을 받지 않아 다른 주소로 위조할 수 없게 한다.
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
			// MailException을 서비스 예외로 변환해 호출자가 SMTP 내부 정보를 직접 보지 않게 한다.
			mailSender.send(message);
		} catch (MailException e) {
			throw new EmailVerificationException("인증 메일을 발송하지 못했습니다.", e);
		}
	}
}
