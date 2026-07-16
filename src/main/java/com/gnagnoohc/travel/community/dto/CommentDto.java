package com.gnagnoohc.travel.community.dto;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("comment")
public class CommentDto {
	private Long commentId;           // comment_id  BIGINT  PK, AUTO_INCREMENT
    private String content;           // content     TEXT    NOT NULL
    private Integer depth;            // depth       INT     DEFAULT 0 (0=원댓글, 1=대댓글)
    private Timestamp createdAt;  // created_at  DATETIME
    private Timestamp updatedAt;  // updated_at  DATETIME ON UPDATE
    private Long parentId;            // parent_id   BIGINT  NULL (부모 댓글, 대댓글일 때만)
    private Long postId;              // post_id     BIGINT  NOT NULL (FK → post)
    private Long memberId;            // member_id   BIGINT  NOT NULL (FK → member)
 
    // ── 화면 표시용 (테이블에 없는 조인/조립 값) ──
    private String memberName;        // 작성자 이름 (member 테이블 JOIN)
    private List<CommentDto> replies; // 이 댓글에 달린 대댓글 목록
}
