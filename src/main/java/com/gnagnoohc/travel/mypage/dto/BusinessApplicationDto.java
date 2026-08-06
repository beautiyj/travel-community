package com.gnagnoohc.travel.mypage.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BusinessApplicationDto {

    private Long applicationId;
    private Long memberId;
    private String businessNumber;
    private String businessName;
    private String representativeName;
    private String businessAddress;
    private String documentUrl;
    private String status;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
