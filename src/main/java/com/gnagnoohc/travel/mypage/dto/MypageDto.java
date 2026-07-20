package com.gnagnoohc.travel.mypage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("member")
public class MypageDto {

	private Long memberId;
	private String name;
	private String loginId;
	private String nickname;
	private Integer memberType;

	private String phone;
	private String gender;
	private LocalDate birth;

	private String profileImgUrl;
	private String signupType;
	private String memberStatus;

	private String emailVerified;
	private LocalDateTime emailVerifiedAt;
	private LocalDateTime lastLoginAt;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	private String password;
	private String currentPassword;
	private String newPassword;
	private String newPasswordCheck;

	private Long reservationId;
	private Long placeId;
	private String visitorName;
	private LocalDate visitDate;
	private Integer headcount;
	private String status;
	
}