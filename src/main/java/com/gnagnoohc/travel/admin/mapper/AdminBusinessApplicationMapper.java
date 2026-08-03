package com.gnagnoohc.travel.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.gnagnoohc.travel.admin.dto.AdminBusinessApplicationDto;
import com.gnagnoohc.travel.admin.dto.AdminDashboardSummaryDto;

@Mapper
public interface AdminBusinessApplicationMapper {

	int countActiveAdmin(@Param("adminMemberId") int adminMemberId);

	AdminDashboardSummaryDto findDashboardSummary();

	List<AdminBusinessApplicationDto> findAllApplications();

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
