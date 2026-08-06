package com.gnagnoohc.travel.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 업소 등록/수정 폼의 부가정보 입력칸 한 줄(라벨 + 안내문구 + 샘플값).
 * BusinessExtraInfoCatalog가 업종별로 이 목록을 만들어 JSP에 내려준다.
 *
 * record가 아니라 getter를 가진 클래스인 이유: JSP EL(${opt.label})은 getLabel() 형태의
 * 접근자만 인식하고 record의 label() 접근자는 읽지 못한다.
 */
@Getter
@AllArgsConstructor
public class BusinessExtraInfoOptionDto {
    // extra_info에 "[라벨] 값"으로 저장될 때의 라벨. 공공데이터 배치와 표기가 같아야 해서 임의로 바꾸면 안 된다.
    private String label;
    // 입력칸 아래에 보여줄 설명
    private String description;
    // placeholder로 보여줄 샘플 입력값 (사업자에게 주는 작성 가이드)
    private String sample;
}
