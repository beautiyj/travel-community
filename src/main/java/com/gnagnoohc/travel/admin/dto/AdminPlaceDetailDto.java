package com.gnagnoohc.travel.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminPlaceDetailDto {
    private Long placeId;
    private String name;
    private Integer placeType;
    private Long regionId;
    private String regionName;
    private String address;
    private String description;
    private boolean closed;
    private List<String> images;
}
