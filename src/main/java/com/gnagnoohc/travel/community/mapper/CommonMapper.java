package com.gnagnoohc.travel.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.community.dto.PlaceTagDto;

// ── 장소 태그 (place 테이블) ──
// community(글쓰기/수정 시 장소 검색)와 comment(댓글 권한 체크) 양쪽에서
// 같이 사용하는 쿼리라서 어느 한쪽 도메인에 두지 않고 common으로 분리함
@Mapper
public interface CommonMapper {

	// 장소 태그: 이름으로 장소 검색 (일반후기 글쓰기/수정 시 검색 모달에서 사용 - 전체 장소 대상)
	// fetchSize는 "더보기" 여부 판단을 위해 실제 페이지 크기보다 1건 더 많이 요청함 (CommonService에서 처리)
	List<PlaceTagDto> searchPlaces(@Param("keyword") String keyword, @Param("offset") int offset, @Param("fetchSize") int fetchSize);

	// 장소 태그: 방문자인증후기용 - 로그인 회원이 이용완료(COMPLETED)한 예약의 장소만 이름으로 검색
	List<PlaceTagDto> searchConfirmedPlaces(@Param("memberId") int memberId, @Param("keyword") String keyword, @Param("offset") int offset, @Param("fetchSize") int fetchSize);

	// 댓글 권한 체크용: 게시글에 태그된 place의 소유주 member_id (place 미태그 글이면 null)
	Integer selectPlaceOwnerId(int postId);
}