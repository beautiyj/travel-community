package com.gnagnoohc.travel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gnagnoohc.travel.auth.mapper.AuthMapper;
import com.gnagnoohc.travel.auth.model.EmailVerification;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

	@Mock
	private AuthMapper mapper;

	@Mock
	private GoogleMailService googleMailService;

	@Mock
	private PasswordEncoder passwordEncoder;

	private EmailVerificationService service;

	@BeforeEach
	void setUp() {
		service = new EmailVerificationService(mapper, googleMailService, passwordEncoder);
	}

	@Test
	void incorrectCodeIncrementsAttemptAndReturnsFalse() {
		EmailVerification verification = verificationExpiringAt(Instant.now().plusSeconds(300));
		when(mapper.findLatestEmailVerificationForUpdate("user@example.com", "SIGNUP"))
				.thenReturn(verification);
		when(mapper.incrementEmailVerificationAttempt(1L)).thenReturn(1);
		when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(false);

		boolean result = service.verifySignupCode("user@EXAMPLE.COM", "123456");

		assertThat(result).isFalse();
		verify(mapper).incrementEmailVerificationAttempt(1L);
		verify(mapper, never()).markEmailVerificationVerified(1L);
	}

	@Test
	void correctCodeMarksVerificationAsVerified() {
		EmailVerification verification = verificationExpiringAt(Instant.now().plusSeconds(300));
		when(mapper.findLatestEmailVerificationForUpdate("user@example.com", "SIGNUP"))
				.thenReturn(verification);
		when(mapper.incrementEmailVerificationAttempt(1L)).thenReturn(1);
		when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
		when(mapper.markEmailVerificationVerified(1L)).thenReturn(1);

		boolean result = service.verifySignupCode("user@example.com", "123456");

		assertThat(result).isTrue();
		verify(mapper).markEmailVerificationVerified(1L);
	}

	private EmailVerification verificationExpiringAt(Instant expiresAt) {
		EmailVerification verification = new EmailVerification();
		verification.setEmail_verification_id(1L);
		verification.setCode_hash("encoded-code");
		verification.setExpires_at(Timestamp.from(expiresAt));
		return verification;
	}
}
