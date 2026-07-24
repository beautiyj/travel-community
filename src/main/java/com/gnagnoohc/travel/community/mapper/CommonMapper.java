package com.gnagnoohc.travel.community.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// ── 장소 태그 (place 테이블) ──
// community(글쓰기/수정 시 장소 검색)와 comment(댓글 권한 체크) 양쪽에서
// 같이 사용하는 쿼리라서 어느 한쪽 도메인에 두지 않고 common으로 분리함
@Mapper
public interface CommonMapper {

	// 장소 태그: 이름으로 장소 검색 (일반후기 글쓰기/수정 시 검색 모달에서 사용 - 전체 장소 대상)
	List<Map<String, Object>> searchPlaces(String keyword);

	// 장소 태그: 방문자인증후기용 - 로그인 회원이 확정(결제완료)한 예약의 장소만 이름으로 검색
	// TODO: 마이페이지 결제완료 로직 완성 전까지 status='PAID' 기준으로 임시 구현. 완성되면 교체 필요.
	List<Map<String, Object>> searchConfirmedPlaces(@Param("memberId") int memberId, @Param("keyword") String keyword);

	// 댓글 권한 체크용: 게시글에 태그된 place의 소유주 member_id (place 미태그 글이면 null)
	Integer selectPlaceOwnerId(int postId);
}