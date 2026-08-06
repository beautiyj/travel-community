package com.gnagnoohc.travel.mypage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("mymember")
public class MypageDto {

	private Long memberId;
	private String name;
	private String loginId;
	private String nickname;
	private String email;
	private Integer memberType;
	private String memberRole;

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

	public LocalDate getCreatedDate() {
		return createdAt == null ? null : createdAt.toLocalDate();
	}

	private String password;
	private String currentPassword;
	private String newPassword;
	private String newPasswordCheck;

	private Long reservationId;
	private Long placeId;
	private String placeName;
	private String visitorName;
	private LocalDate visitDate;
	private Integer headcount;
	private String status;
	private Integer amount;
	
	private Long wishlistId;
	
}
