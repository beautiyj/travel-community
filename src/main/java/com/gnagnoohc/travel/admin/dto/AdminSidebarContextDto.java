package com.gnagnoohc.travel.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminSidebarContextDto {
    private Long placeId;
    private String placeName;
    private String ownerName;
    private boolean isClosed;
    private Integer pendingCount;
    private Integer cancelRequestCount;
}
