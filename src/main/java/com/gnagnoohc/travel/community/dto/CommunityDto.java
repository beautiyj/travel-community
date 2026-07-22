package com.gnagnoohc.travel.community.dto;

import java.sql.Date;
import java.util.List;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("community")
public class CommunityDto {
	private int postId;               // post_id      INT           PK, AUTO_INCREMENT
    private int memberId;             // member_id    INT           NOT NULL
    private Integer placeId;          // place_id     INT           NULL (NULL 가능이라 int 대신 Integer 유지)
    private String title;             // title        VARCHAR(200) NOT NULL
    private String content;           // content      TEXT         NOT NULL
    private Date createdAt;  		// created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP
    private Date updatedAt;  		// updated_at   DATETIME     ON UPDATE CURRENT_TIMESTAMP
    private Integer readcount;        // readcount    INT          DEFAULT 0
    private String category;          // category     VARCHAR(200) NULL
    
    // ── 화면 표시용 (post 테이블에 없는 조인/조립 값) ──
    private String nickname;               // 작성자 이름 (member 테이블 JOIN으로 채움)
    private String thumbnailUrl;           // 목록 썸네일 (image 테이블에서 sort_order=0 인 이미지, 없으면 null)
    private List<ImageDto> imageList;       // 이미지 목록
    private List<CommentDto> commentList;   // 댓글 목록
    
    public enum PostCategory {
        NORMAL("일반", "일반", "자유로운 여행 이야기"),
        RECRUIT("모집", "모집(동행)", "동행자를 구하는 글"),
        GENERAL_REVIEW("일반후기", "일반후기", "다녀온 여행 후기"),
        VERIFIED_REVIEW("방문자인증후기", "방문자인증후기", "방문 인증 후 남기는 후기");

        private final String value;         // DB(post.category)에 실제로 저장되는 값
        private final String displayLabel;  // 화면에 보여줄 텍스트
        private final String description;   // 글쓰기 화면 카드 설명

        PostCategory(String value, String displayLabel, String description) {
            this.value = value;
            this.displayLabel = displayLabel;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayLabel() {
            return displayLabel;
        }

        public String getDescription() {
            return description;
        }

        // 저장된 값(value) → enum으로 역변환 (유효성 검사용)
        public static PostCategory fromValue(String value) {
            for (PostCategory c : values()) {
                if (c.value.equals(value)) return c;
            }
            throw new IllegalArgumentException("존재하지 않는 카테고리: " + value);
        }
    }
}