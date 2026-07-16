-- REGION (지역 분류 체계) 테이블
CREATE TABLE REGION (
    region_id        BIGINT NOT NULL,
    region_name      VARCHAR(50) NOT NULL,
    parent_region_id BIGINT,
    
    CONSTRAINT PK_REGION PRIMARY KEY (region_id),
    
    -- 셀프 참조 외래키 설정 (부모 지역 번호)
    CONSTRAINT FK_REGION_PARENT FOREIGN KEY (parent_region_id) 
        REFERENCES REGION (region_id) ON DELETE SET NULL
);

-- PLACE (여행지 / 맛집 / 숙박 통합 관리) 테이블
CREATE TABLE PLACE (
    place_id        BIGINT NOT NULL AUTO_INCREMENT,
    content_id      VARCHAR(20) NOT NULL,
    content_type_id VARCHAR(10),
    place_type      TINYINT NOT NULL,
    region_id       BIGINT,
    member_id       BIGINT,                           -- MEMBER 테이블이 생성되어 있다고 가정
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    address         VARCHAR(500),
    mapx            DECIMAL(12, 9),
    mapy            DECIMAL(12, 9),
    is_closed       BOOLEAN DEFAULT FALSE NOT NULL,   -- 0: 영업중 1: 휴,폐업
    admin_type      INT NOT NULL DEFAULT 0,           -- 0: 공공데이터 시드(기본값), 1: 사업자 등록/클레임
    first_image     VARCHAR(500),                     -- 이미지 테이블의 조인 방지용 필드
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_PLACE PRIMARY KEY (place_id),
    
    -- 공공데이터 고유 ID 중복 방지 유니크 제약조건
    CONSTRAINT UQ_PLACE_CONTENT_ID UNIQUE (content_id),
    
    -- 외래키 설정 (지역 테이블 연계)
    CONSTRAINT FK_PLACE_REGION FOREIGN KEY (region_id) 
        REFERENCES REGION (region_id) ON DELETE SET NULL

    -- admin_type 값 무결성 제약조건 (공공데이터/사업자회원 직접등록 - 0 또는 1만 허용)
    CONSTRAINT CK_PLACE_ADMIN_TYPE CHECK (admin_type IN (0, 1))

    -- 멤버테이블 생성 시 추가해두기        
    -- , CONSTRAINT FK_PLACE_MEMBER FOREIGN KEY (member_id) REFERENCES MEMBER (member_id) ON DELETE SET NULL
);

-- PLACE_IMAGE (상세페이지용 서브 사진첩) 테이블
CREATE TABLE PLACE_IMAGE (
    image_id        BIGINT NOT NULL AUTO_INCREMENT,
    place_id        BIGINT NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    sort_order      INT DEFAULT 0 NOT NULL,

    CONSTRAINT PK_PLACE_IMAGE PRIMARY KEY (image_id),
    
    -- 외래키 설정 (PK - place_id와 연계 & PLACE 삭제 시 이미지도 연쇄 삭제)
    CONSTRAINT FK_PLACE_IMAGE_TO_PLACE FOREIGN KEY (place_id) 
        REFERENCES PLACE (place_id) ON DELETE CASCADE
);