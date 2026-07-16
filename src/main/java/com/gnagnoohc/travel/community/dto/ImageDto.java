package com.gnagnoohc.travel.community.dto;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("image")
public class ImageDto {
	private Long imageId;             // image_id    BIGINT       PK, AUTO_INCREMENT
    private Long postId2;             // post_id2    BIGINT       NOT NULL (FK → post.post_id)
    private String imageUrl;          // image_url   VARCHAR(500) NOT NULL
    private Integer sortOrder;        // sort_order  INT          DEFAULT 0
    private Timestamp createdAt;  // created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
}
