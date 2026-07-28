-- =========================================================
-- Travel Community 전체 스키마
-- · 식별자 통일: member_id / place_id / region_id / post_id(REVIEW_ANALYSIS) = INT
-- · 각 도메인 PK 중 reservation_id / payment_id / wishlist_id / login_history_id
--   / PLACE_IMAGE.image_id 는 BIGINT 유지
-- · 순서: 테이블 생성 → PK/AUTO_INCREMENT → UNIQUE → CHECK → FK → INDEX
-- =========================================================
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- member : 회원 공통 정보
-- =========================================================
CREATE TABLE member (
    member_id          INT           NOT NULL,          -- PK
    name               VARCHAR(20)   NOT NULL,
    login_id           VARCHAR(100)  NOT NULL,
    nickname           VARCHAR(20)   NOT NULL,
    email              VARCHAR(100)  NOT NULL,
    member_type        TINYINT       NOT NULL,          -- 0 관리자 / 1 일반 / 2 사업자
    phone              VARCHAR(20)   NULL,
    gender             VARCHAR(10)   NULL,
    birth              DATE          NULL,
    profile_img_url    VARCHAR(500)  NULL,
    signup_type        VARCHAR(20)   NOT NULL,          -- 최초 가입 경로
    member_status      VARCHAR(20)   NOT NULL DEFAULT 'INACTIVE',
    member_role        VARCHAR(20)   NOT NULL DEFAULT 'USER',
    email_verified     CHAR(1)       NOT NULL DEFAULT 'N',
    email_verified_at  DATETIME      NULL,
    last_login_at      DATETIME      NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME      NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE member ADD PRIMARY KEY (member_id);
ALTER TABLE member MODIFY member_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE member ADD CONSTRAINT uq_member_login_id UNIQUE (login_id);
ALTER TABLE member ADD CONSTRAINT uq_member_nickname UNIQUE (nickname);

ALTER TABLE member ADD CONSTRAINT chk_member_type      CHECK (member_type IN (0,1,2));
ALTER TABLE member ADD CONSTRAINT chk_member_role      CHECK (member_role IN ('USER','BUSINESS','ADMIN'));
ALTER TABLE member ADD CONSTRAINT chk_member_type_role CHECK (
        (member_type=0 AND member_role='ADMIN')
     OR (member_type=1 AND member_role='USER')
     OR (member_type=2 AND member_role IN ('USER','BUSINESS')));
ALTER TABLE member ADD CONSTRAINT chk_member_signup_type    CHECK (signup_type IN ('LOCAL','GOOGLE','KAKAO','NAVER'));
ALTER TABLE member ADD CONSTRAINT chk_member_status         CHECK (member_status IN ('INACTIVE','ACTIVE','SUSPENDED','WITHDRAWN'));
ALTER TABLE member ADD CONSTRAINT chk_member_email_verified CHECK (email_verified IN ('Y','N'));
ALTER TABLE member ADD CONSTRAINT chk_member_email_verified_at CHECK (
        (email_verified='N' AND email_verified_at IS NULL)
     OR (email_verified='Y' AND email_verified_at IS NOT NULL));
ALTER TABLE member ADD CONSTRAINT chk_member_gender    CHECK (gender IS NULL OR gender IN ('MALE','FEMALE'));
ALTER TABLE member ADD CONSTRAINT chk_member_withdrawn CHECK (
        (member_status='WITHDRAWN' AND deleted_at IS NOT NULL)
     OR (member_status<>'WITHDRAWN' AND deleted_at IS NULL));


-- =========================================================
-- REGION : 지역 (자기참조 계층)
-- =========================================================
CREATE TABLE REGION (
    region_id         INT          NOT NULL,             -- PK
    region_name       VARCHAR(50)  NOT NULL,
    parent_region_id  INT          NULL                  -- 상위 지역(자기참조)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE REGION ADD PRIMARY KEY (region_id);
ALTER TABLE REGION MODIFY region_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE REGION ADD CONSTRAINT FK_REGION_PARENT
    FOREIGN KEY (parent_region_id) REFERENCES REGION (region_id) ON DELETE SET NULL;


-- =========================================================
-- member_local_auth : 로컬 로그인 인증
-- =========================================================
CREATE TABLE member_local_auth (
    username             VARCHAR(20)  NOT NULL,          -- PK(=로그인 아이디)
    password_hash        VARCHAR(255) NOT NULL,
    password_updated_at  DATETIME     NULL,
    failed_login_count   INT          NOT NULL DEFAULT 0,
    locked_until         DATETIME     NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    member_id            INT          NOT NULL           -- 회원(1:1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE member_local_auth ADD PRIMARY KEY (username);
ALTER TABLE member_local_auth ADD CONSTRAINT uq_member_local_auth_member   UNIQUE (member_id);
ALTER TABLE member_local_auth ADD CONSTRAINT chk_member_local_failed_count CHECK (failed_login_count >= 0);
ALTER TABLE member_local_auth ADD CONSTRAINT fk_member_local_auth_member
    FOREIGN KEY (member_id) REFERENCES member (member_id) ON UPDATE RESTRICT ON DELETE CASCADE;


-- =========================================================
-- member_social_auth : 소셜 로그인 연동
-- =========================================================
CREATE TABLE member_social_auth (
    social_auth_id               INT          NOT NULL,  -- PK
    provider                     VARCHAR(20)  NOT NULL,  -- GOOGLE / KAKAO / NAVER
    provider_user_id             VARCHAR(255) NOT NULL,
    provider_email               VARCHAR(100) NULL,
    provider_email_verified_yn   CHAR(1)      NOT NULL DEFAULT 'N',
    provider_name                VARCHAR(100) NULL,
    provider_nickname            VARCHAR(100) NULL,
    provider_profile_image_url   VARCHAR(500) NULL,
    last_login_at                DATETIME     NULL,
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    member_id                    INT          NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE member_social_auth ADD PRIMARY KEY (social_auth_id);
ALTER TABLE member_social_auth MODIFY social_auth_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE member_social_auth ADD CONSTRAINT uq_social_provider_user   UNIQUE (provider, provider_user_id);
ALTER TABLE member_social_auth ADD CONSTRAINT uq_member_social_provider UNIQUE (member_id, provider);

ALTER TABLE member_social_auth ADD CONSTRAINT chk_social_provider       CHECK (provider IN ('GOOGLE','KAKAO','NAVER'));
ALTER TABLE member_social_auth ADD CONSTRAINT chk_social_email_verified CHECK (provider_email_verified_yn IN ('Y','N'));
ALTER TABLE member_social_auth ADD CONSTRAINT chk_social_verified_email CHECK (
        provider_email_verified_yn = 'N' OR provider_email IS NOT NULL);

ALTER TABLE member_social_auth ADD CONSTRAINT fk_member_social_auth_member
    FOREIGN KEY (member_id) REFERENCES member (member_id) ON UPDATE RESTRICT ON DELETE CASCADE;


-- =========================================================
-- email_verification : 이메일 인증 (resend_count 없음, consumed_at·request_ip 있음)
-- =========================================================
CREATE TABLE email_verification (
    email_verification_id  INT          NOT NULL,        -- PK
    email                  VARCHAR(100) NOT NULL,
    purpose                VARCHAR(30)  NOT NULL,        -- SIGNUP / FIND_PASSWORD 등
    code_hash              VARCHAR(255) NOT NULL,
    expires_at             DATETIME     NOT NULL,
    verified_at            DATETIME     NULL,
    attempt_count          INT          NOT NULL DEFAULT 0,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_id              INT          NULL,            -- 가입:사용 시 / 비번찾기:생성 시 설정
    consumed_at            DATETIME     NULL,            -- 인증 결과 1회성 소비
    request_ip             VARCHAR(45)  NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE email_verification ADD PRIMARY KEY (email_verification_id);
ALTER TABLE email_verification MODIFY email_verification_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE email_verification ADD CONSTRAINT chk_email_verification_purpose CHECK (
        purpose IN ('SIGNUP','PASSWORD_CHANGE','FIND_PASSWORD','MEMBER_WITHDRAW'));
ALTER TABLE email_verification ADD CONSTRAINT chk_email_verification_attempt    CHECK (attempt_count >= 0);
ALTER TABLE email_verification ADD CONSTRAINT chk_email_verification_expiration CHECK (expires_at > created_at);
ALTER TABLE email_verification ADD CONSTRAINT chk_email_verification_consumed   CHECK (
        consumed_at IS NULL OR verified_at IS NOT NULL);

ALTER TABLE email_verification ADD CONSTRAINT fk_email_verification_member
    FOREIGN KEY (member_id) REFERENCES member (member_id) ON UPDATE RESTRICT ON DELETE SET NULL;


-- =========================================================
-- business_application : 사업자 승인 신청 (회원당 1건)
-- =========================================================
CREATE TABLE business_application (
    member_id                       INT          NOT NULL,  -- PK = 신청 회원
    business_registration_img_url   VARCHAR(500) NOT NULL,
    application_status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    applied_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at                     DATETIME     NULL,
    approved_by                     INT          NULL        -- 승인 관리자 member_id
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE business_application ADD PRIMARY KEY (member_id);

ALTER TABLE business_application ADD CONSTRAINT chk_business_application_status CHECK (
        application_status IN ('PENDING','APPROVED'));
ALTER TABLE business_application ADD CONSTRAINT chk_business_application_approval CHECK (
        (application_status='PENDING'  AND approved_at IS NULL     AND approved_by IS NULL)
     OR (application_status='APPROVED' AND approved_at IS NOT NULL AND approved_by IS NOT NULL));

ALTER TABLE business_application ADD CONSTRAINT fk_business_application_member
    FOREIGN KEY (member_id)  REFERENCES member (member_id) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE business_application ADD CONSTRAINT fk_business_application_admin
    FOREIGN KEY (approved_by) REFERENCES member (member_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


-- =========================================================
-- login_history : 로그인 이력 (성공/실패 감사 로그)
-- =========================================================
CREATE TABLE login_history (
    login_history_id  BIGINT       NOT NULL,             -- PK
    login_type        VARCHAR(20)  NOT NULL,             -- LOCAL / SOCIAL 등
    provider          VARCHAR(20)  NULL,
    username          VARCHAR(50)  NULL,
    success_yn        CHAR(1)      NOT NULL,
    failure_reason    VARCHAR(100) NULL,
    ip_address        VARCHAR(45)  NULL,
    user_agent        VARCHAR(500) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_id         INT          NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE login_history ADD PRIMARY KEY (login_history_id);
ALTER TABLE login_history MODIFY login_history_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE login_history ADD CONSTRAINT fk_login_history_member
    FOREIGN KEY (member_id) REFERENCES member (member_id);


-- =========================================================
-- PLACE : 장소 (content_id / content_type_id / admin_type 삭제됨)
-- =========================================================
CREATE TABLE PLACE (
    place_id      INT           NOT NULL,                -- PK
    member_id     INT           NULL,                    -- 소유주(사업자), 시드는 NULL
    region_id     INT           NULL,                    -- 지역
    place_type    TINYINT       NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    description   TEXT          NULL,
    address       VARCHAR(255)  NULL,
    mapx          DECIMAL(12,9) NULL,                    -- 경도
    mapy          DECIMAL(12,9) NULL,                    -- 위도
    is_closed     TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    first_image   VARCHAR(500)  NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE PLACE ADD PRIMARY KEY (place_id);
ALTER TABLE PLACE MODIFY place_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE PLACE ADD CONSTRAINT FK_PLACE_MEMBER FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE SET NULL;
ALTER TABLE PLACE ADD CONSTRAINT FK_PLACE_REGION FOREIGN KEY (region_id) REFERENCES REGION (region_id) ON DELETE SET NULL;


-- =========================================================
-- PLACE_IMAGE : 장소 이미지
-- =========================================================
CREATE TABLE PLACE_IMAGE (
    image_id    BIGINT       NOT NULL,                   -- PK
    place_id    INT          NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE PLACE_IMAGE ADD PRIMARY KEY (image_id);
ALTER TABLE PLACE_IMAGE MODIFY image_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE PLACE_IMAGE ADD CONSTRAINT FK_PLACE_IMAGE_TO_PLACE
    FOREIGN KEY (place_id) REFERENCES PLACE (place_id) ON DELETE CASCADE;


-- =========================================================
-- RESERVATION : 예약 (status 영어 저장)
-- =========================================================
CREATE TABLE RESERVATION (
    reservation_id       BIGINT       NOT NULL,          -- PK
    member_id            INT          NOT NULL,
    place_id             INT          NOT NULL,
    visitor_name         VARCHAR(50)  NOT NULL,
    phone                VARCHAR(20)  NOT NULL,
    visit_date           DATE         NOT NULL,
    headcount            INT          NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancel_reason        VARCHAR(100) NULL,
    cancel_requested_at  DATETIME     NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE RESERVATION ADD PRIMARY KEY (reservation_id);
ALTER TABLE RESERVATION MODIFY reservation_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE RESERVATION ADD CONSTRAINT FK_RESERVATION_MEMBER FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE RESERVATION ADD CONSTRAINT FK_RESERVATION_PLACE  FOREIGN KEY (place_id)  REFERENCES PLACE (place_id);

CREATE INDEX IDX_RESERVATION_SLOT   ON RESERVATION (member_id, place_id, visit_date, status);
CREATE INDEX IDX_RESERVATION_STATUS ON RESERVATION (status);


-- =========================================================
-- PAYMENT : 결제 (예약당 1건 / 주문번호 유일)
-- =========================================================
CREATE TABLE PAYMENT (
    payment_id       BIGINT       NOT NULL,              -- PK
    reservation_id   BIGINT       NOT NULL,
    amount           INT          NOT NULL,
    payment_key      VARCHAR(200) NOT NULL,              -- 토스 paymentKey / 카카오 tid
    payment_status   VARCHAR(20)  NOT NULL,              -- DONE / CANCELED / FAILED
    order_id         VARCHAR(100) NOT NULL,
    paid_at          DATETIME     NULL,
    payment_type     TINYINT      NOT NULL               -- 1 토스 / 2 카카오
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE PAYMENT ADD PRIMARY KEY (payment_id);
ALTER TABLE PAYMENT MODIFY payment_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE PAYMENT ADD CONSTRAINT UQ_PAYMENT_ORDER_ID    UNIQUE (order_id);
ALTER TABLE PAYMENT ADD CONSTRAINT UQ_PAYMENT_RESERVATION UNIQUE (reservation_id);
ALTER TABLE PAYMENT ADD CONSTRAINT FK_PAYMENT_RESERVATION
    FOREIGN KEY (reservation_id) REFERENCES RESERVATION (reservation_id);


-- =========================================================
-- post : 커뮤니티 게시글 (nickname 컬럼 없음 = member JOIN)
-- =========================================================
CREATE TABLE post (
    post_id     INT          NOT NULL,                   -- PK
    member_id   INT          NOT NULL,
    place_id    INT          NULL,                        -- 방문자인증후기 장소 태그
    title       VARCHAR(50) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    readcount   INT          NOT NULL DEFAULT 0,
    category    VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE post ADD PRIMARY KEY (post_id);
ALTER TABLE post MODIFY post_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE post ADD CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE post ADD CONSTRAINT fk_post_place  FOREIGN KEY (place_id)  REFERENCES PLACE (place_id);

CREATE INDEX idx_post_category_created ON post (category, created_at DESC);


-- =========================================================
-- post_image : 게시글 이미지 (구 image 테이블 대체)
-- =========================================================
CREATE TABLE post_image (
    image_id    INT          NOT NULL,                   -- PK
    post_id     INT          NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE post_image ADD PRIMARY KEY (image_id);
ALTER TABLE post_image MODIFY image_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE post_image ADD CONSTRAINT fk_post_image_post
    FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE;

CREATE INDEX idx_post_image_sort ON post_image (post_id, sort_order);


-- =========================================================
-- comment : 댓글 / 대댓글 (parent_id 자기참조)
-- =========================================================
CREATE TABLE comment (
    comment_id  INT       NOT NULL,                      -- PK
    content     TEXT      NOT NULL,
    depth       INT       NOT NULL DEFAULT 0,            -- 0 원댓글 / 1 대댓글
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME  NULL ON UPDATE CURRENT_TIMESTAMP,
    parent_id   INT       NULL,                          -- 부모 댓글(자기참조)
    post_id     INT       NOT NULL,
    member_id   INT       NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE comment ADD PRIMARY KEY (comment_id);
ALTER TABLE comment MODIFY comment_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE comment ADD CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES post (post_id)      ON DELETE CASCADE;
ALTER TABLE comment ADD CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (comment_id) ON DELETE CASCADE;


-- =========================================================
-- WISHLIST : 찜
-- =========================================================
CREATE TABLE WISHLIST (
    wishlist_id  BIGINT   NOT NULL,                      -- PK
    member_id    INT      NOT NULL,
    place_id     INT      NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE WISHLIST ADD PRIMARY KEY (wishlist_id);
ALTER TABLE WISHLIST MODIFY wishlist_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE WISHLIST ADD CONSTRAINT wishlist_ibfk_1 FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE WISHLIST ADD CONSTRAINT wishlist_ibfk_2 FOREIGN KEY (place_id)  REFERENCES PLACE (place_id);


-- =========================================================
-- REVIEW_ANALYSIS : 리뷰 감성분석 결과 (post_id / place_id INT 통일 + FK 추가)
-- =========================================================
CREATE TABLE REVIEW_ANALYSIS (
    post_id      INT      NOT NULL,                      -- PK = 게시글(1:1)
    place_id     INT      NOT NULL,
    sentiment    TINYINT  NOT NULL,                      -- 감성 분류 값
    keywords     JSON     NULL,                          -- 추출 키워드
    analyzed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE REVIEW_ANALYSIS ADD PRIMARY KEY (post_id);

ALTER TABLE REVIEW_ANALYSIS ADD CONSTRAINT fk_review_analysis_post
    FOREIGN KEY (post_id)  REFERENCES post (post_id) ON DELETE CASCADE;
ALTER TABLE REVIEW_ANALYSIS ADD CONSTRAINT fk_review_analysis_place
    FOREIGN KEY (place_id) REFERENCES PLACE (place_id);


SET FOREIGN_KEY_CHECKS = 1;
