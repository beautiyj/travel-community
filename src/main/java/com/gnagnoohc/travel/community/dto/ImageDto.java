package com.gnagnoohc.travel.community.dto;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("image")
public class ImageDto {
	private int imageId;              // image_id    INT           PK, AUTO_INCREMENT
    private int postId;               // post_id     INT           NOT NULL (FK → post.post_id)
    private String imageUrl;          // image_url   VARCHAR(500) NOT NULL
    private int sortOrder;            // sort_order  INT          DEFAULT 0
    private Timestamp createdAt;  // created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
}