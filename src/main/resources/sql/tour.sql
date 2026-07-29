-- REGION (지역 분류 체계) 테이블
CREATE TABLE REGION (
    region_id        INT NOT NULL,
    region_name      VARCHAR(50) NOT NULL,
    parent_region_id INT,
    
    CONSTRAINT PK_REGION PRIMARY KEY (region_id),
    
    -- 셀프 참조 외래키 설정 (부모 지역 번호 삭제 시 NULL 처리)
    CONSTRAINT FK_REGION_PARENT FOREIGN KEY (parent_region_id) 
        REFERENCES REGION (region_id) ON DELETE SET NULL
);

-- PLACE (여행지 / 맛집 / 숙박 통합 관리) 테이블
CREATE TABLE PLACE (
    place_id        INT NOT NULL,
    member_id       INT,                           -- 외래키 (공공데이터용 시스템 유령계정 & 사업자 회원 PK)
    region_id       INT NOT NULL,                           -- 외래키
    
    place_type      VARCHAR(10) NOT NULL,                   -- tour / food / stay
    name            VARCHAR(200) NOT NULL,                  -- 공공데이터 title -> name
    description     TEXT,                                   -- 공공데이터 overview
    address         VARCHAR(500),                           -- 주소 (addr1 + addr2)
    mapx            DECIMAL(12, 9),                         -- 경도
    mapy            DECIMAL(12, 9),                         -- 위도
    is_closed       BOOLEAN DEFAULT FALSE NOT NULL,         -- 기본값(FALSE) 0: 영업중, 1: 휴/폐업
    
    first_image     VARCHAR(500),                           -- 대표 이미지 URL (조인 방지용 카드/목록 썸네일)
    hashtags        VARCHAR(300),                           -- 가공 해시태그
    min_price       INT,                                    -- 검색/필터링/정렬용 최저가 (숫자 연산용)
    use_fee_info    VARCHAR(1000),                          -- 화면 표시용 요금 안내 원문
    people_count    INT DEFAULT 1 NOT NULL,                 -- 정원 / 기준 인원 수 (기본값 1)
    
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_PLACE PRIMARY KEY (place_id),
    CONSTRAINT FK_PLACE_REGION FOREIGN KEY (region_id) 
        REFERENCES REGION (region_id) ON DELETE CASCADE,
    -- 멤버 테이블 생성 후 적용할 것:
    CONSTRAINT FK_PLACE_MEMBER FOREIGN KEY (member_id) 
        REFERENCES MEMBER (member_id) ON DELETE SET NULL
);

-- PLACE_IMAGE (상세페이지용 서브 사진첩) 테이블
CREATE TABLE PLACE_IMAGE (
    image_id        BIGINT NOT NULL AUTO_INCREMENT,
    place_id        INT NOT NULL,                           -- PLACE 테이블 참조 (FK)
    image_url       VARCHAR(500) NOT NULL,
    sort_order      INT DEFAULT 0 NOT NULL,                 -- 정렬 순서 (0: 대표 썸네일이미지는 원본, Place테이블은 썸네일)

    CONSTRAINT PK_PLACE_IMAGE PRIMARY KEY (image_id),
    
    -- 외래키 설정 (PLACE 삭제 시 관련 이미지 전체 연쇄 삭제)
    CONSTRAINT FK_PLACE_IMAGE_TO_PLACE FOREIGN KEY (place_id) 
        REFERENCES PLACE (place_id) ON DELETE CASCADE
);