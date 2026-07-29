package com.gnagnoohc.travel.community.dto;

import java.sql.Timestamp;

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
    private Timestamp deletedAt;  // deleted_at  DATETIME NULL (소프트 삭제 시각, NULL이면 살아있는 댓글)
    private Integer parentId;         // parent_id   INT      NULL (부모 댓글일 때만 값 있음, top-level은 null → int 대신 Integer 유지)
    private int postId;               // post_id     INT      NOT NULL (FK → post)
    private int memberId;             // member_id   INT      NOT NULL (FK → member)

    // ── 화면 표시용 (테이블에 없는 조인/조립 값) ──
    private String memberName;        // 작성자 이름 (member 테이블 JOIN, 사장님이면 "업체명+사장"으로 오버라이드됨)
    private boolean ownerComment;     // 업소 사장님이 자신의 사업장 리뷰에 남긴 댓글인지 (뱃지 표시용)
    private Integer memberType;       // 작성자 회원 유형 (1: 일반, 2: 사업자, member 테이블 JOIN)
    private String profileImgUrl;     // 일반 회원 프로필 사진 (member 테이블 JOIN)
    private String businessImage;     // 사업자 회원의 업소 대표사진 (place.first_image, member_id로 JOIN)

    // 작성자 아바타로 표시할 이미지 URL 결정 (CommunityDto.getAvatarUrl()과 동일한 로직)
    // 사업자면 업소 대표사진, 일반 회원이면 개인 프로필 사진, 둘 다 없으면 null(→ JSP에서 기본 아바타 표시)
    public String getAvatarUrl() {
        if (memberType != null && memberType == 2) {
            return businessImage;
        }
        return profileImgUrl;
    }
}