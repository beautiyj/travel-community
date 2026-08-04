package com.gnagnoohc.travel.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.admin.dto.AdminBusinessApplicationDto;
import com.gnagnoohc.travel.admin.dto.AdminDashboardSummaryDto;

/**
 * 관리자 사업자 심사 유스케이스의 SQL 경계다.
 * MyBatis가 이 인터페이스와 같은 namespace의 XML 문장을 연결하고, {@code @Param} 이름을
 * XML의 {@code #{...}}에 제공한다. 변경 메서드의 반환값은 영향받은 행 수이며 서비스가
 * 정확히 한 행이 바뀌었는지 검사한다.
 */
@Mapper
public interface AdminBusinessApplicationMapper {

	int countActiveAdmin(@Param("adminMemberId") int adminMemberId);

	AdminDashboardSummaryDto findDashboardSummary();

	List<AdminBusinessApplicationDto> findApplications(
			@Param("applicationStatus") String applicationStatus);

	AdminBusinessApplicationDto findApplicationById(
			@Param("applicationId") int applicationId);

	String findBusinessRegistrationFileKey(
			@Param("applicationId") int applicationId);

	AdminBusinessApplicationDto findReviewTargetForUpdate(
			@Param("applicationId") int applicationId);

	int approveApplication(
			@Param("applicationId") int applicationId,
			@Param("adminMemberId") int adminMemberId);

	int rejectApplication(
			@Param("applicationId") int applicationId,
			@Param("adminMemberId") int adminMemberId,
			@Param("rejectionReason") String rejectionReason);

	int grantBusinessRole(@Param("memberId") int memberId);
}
