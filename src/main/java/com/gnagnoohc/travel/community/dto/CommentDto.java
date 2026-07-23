package com.gnagnoohc.travel.community.dto;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("comment")
public class CommentDto {
	private int commentId;            // comment_id  INT      PK, AUTO_INCREMENT
    private String content;           // content     TEXT    NOT NULL
    private int depth;                // depth       INT     DEFAULT 0 (0=원댓글, 1=대댓글)
    private Timestamp createdAt;  // created_at  DATETIME
    private Timestamp updatedAt;  // updated_at  DATETIME ON UPDATE
    private Integer parentId;         // parent_id   INT      NULL (부모 댓글일 때만 값 있음, top-level은 null → int 대신 Integer 유지)
    private int postId;               // post_id     INT      NOT NULL (FK → post)
    private int memberId;             // member_id   INT      NOT NULL (FK → member)
 
    // ── 화면 표시용 (테이블에 없는 조인/조립 값) ──
    private String memberName;        // 작성자 이름 (member 테이블 JOIN, 사장님이면 "업체명+사장"으로 오버라이드됨)
    private boolean ownerComment;     // 업소 사장님이 자신의 사업장 리뷰에 남긴 댓글인지 (뱃지 표시용)
    private List<CommentDto> replies; // 이 댓글에 달린 대댓글 목록
}