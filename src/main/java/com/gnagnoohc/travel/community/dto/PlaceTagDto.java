package com.gnagnoohc.travel.community.dto;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("placeTag")
public class PlaceTagDto {
	private int placeId;
	private String name;
	private String placeType;   // "stay" | "food" | "tour"
}
