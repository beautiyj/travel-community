package com.gnagnoohc.travel.community.dto;

import java.sql.Date;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("community")
public class CommunityDto {
	private Long postId;              // post_id      BIGINT       PK, AUTO_INCREMENT
    private Long memberId;            // member_id    BIGINT       NOT NULL
    private Long placeId;             // place_id     BIGINT       NULL
    private String title;             // title        VARCHAR(200) NOT NULL
    private String content;           // content      TEXT         NOT NULL
    private Date createdAt;  		// created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP
    private Date updatedAt;  		// updated_at   DATETIME     ON UPDATE CURRENT_TIMESTAMP
    private Integer readcount;        // readcount    INT          DEFAULT 0
    private String category;          // category     VARCHAR(200) NULL
    
    // ── 화면 표시용 (post 테이블에 없는 조인/조립 값) ──
    private String nickname;               // 작성자 이름 (member 테이블 JOIN으로 채움)
    private List<ImageDto> imageList;       // 이미지 목록
    private List<CommentDto> commentList;   // 댓글 목록
}
