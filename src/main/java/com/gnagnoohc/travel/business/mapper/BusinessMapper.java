package com.gnagnoohc.travel.business.mapper;

import com.gnagnoohc.travel.business.dto.BusinessDashboardCountsDto;
import com.gnagnoohc.travel.business.dto.BusinessMonthlyTrendDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceDetailDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceOverviewDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceRegisterDto;
import com.gnagnoohc.travel.business.dto.BusinessPlaceUpdateDto;
//import com.gnagnoohc.travel.business.dto.BusinessRegionOptionDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationDto;
import com.gnagnoohc.travel.business.dto.BusinessReservationStatusCountsDto;
import com.gnagnoohc.travel.business.dto.BusinessReviewDto;
import com.gnagnoohc.travel.business.dto.BusinessReviewSentimentCountsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessMapper {

    BusinessPlaceOverviewDto selectPlaceOverviewByMember(@Param("bizMemberId") Long bizMemberId);

    List<BusinessReservationDto> selectTodayReservations(@Param("placeId") Long placeId);

    BusinessDashboardCountsDto selectDashboardCounts(@Param("placeId") Long placeId);

    List<BusinessMonthlyTrendDto> selectMonthlyTrend(@Param("placeId") Long placeId);

    List<BusinessReservationDto> selectReservationsByPlace(
            @Param("placeId") Long placeId,
            @Param("bizMemberId") Long bizMemberId,
            @Param("status") String status
    );

    // 예약 관리 화면 상태별 필터 탭에 표시할 개수 (취소요청/확정/완료/취소)
    BusinessReservationStatusCountsDto selectReservationStatusCounts(
            @Param("placeId") Long placeId,
            @Param("bizMemberId") Long bizMemberId
    );

    // 취소 승인/거절 전 소유자 확인
    boolean existsReservationForBizMember(
            @Param("reservationId") Long reservationId,
            @Param("bizMemberId") Long bizMemberId
    );

    int updatePlaceClosed(
            @Param("placeId") Long placeId,
            @Param("bizMemberId") Long bizMemberId,
            @Param("isClosed") boolean isClosed
    );

//    List<BusinessRegionOptionDto> selectRegionOptions();

    String selectMemberRole(@Param("memberId") Long memberId);

    int insertOwnerPlace(BusinessPlaceRegisterDto place);

    int insertPlaceImage(
            @Param("placeId") Long placeId,
            @Param("imageUrl") String imageUrl,
            @Param("sortOrder") int sortOrder
    );

    BusinessPlaceDetailDto selectPlaceDetailByMember(@Param("bizMemberId") Long bizMemberId);

    List<String> selectPlaceImages(@Param("placeId") Long placeId);

    int updatePlace(BusinessPlaceUpdateDto place);

    int deletePlaceImage(@Param("placeId") Long placeId, @Param("imageUrl") String imageUrl);

    // 후기 확인 : 업소에 달린 후기(category='후기') 목록. sentiment가 null이면 전체, 아니면 -1/0/1로 필터링.
    // 답글은 커뮤니티 상세(댓글)에서 다룬다
    List<BusinessReviewDto> selectReviewsByPlace(@Param("placeId") Long placeId, @Param("sentiment") Integer sentiment);

    // 후기 감성분석 : 아직 REVIEW_ANALYSIS에 결과가 없는 후기 (postId, content만 채워짐)
    List<BusinessReviewDto> selectUnanalyzedReviews(@Param("placeId") Long placeId);

    int insertReviewAnalysis(
            @Param("postId") Long postId,
            @Param("placeId") Long placeId,
            @Param("sentiment") int sentiment,
            @Param("keywordsJson") String keywordsJson
    );

    BusinessReviewSentimentCountsDto selectSentimentCounts(@Param("placeId") Long placeId);

    // 워드클라우드용 : 분석완료된 후기들의 keywords(JSON) 원본. 집계는 Java 쪽에서 수행
    List<String> selectAnalyzedKeywordsJsonByPlace(@Param("placeId") Long placeId);
}
