package com.auth.models.dtos.dashboardAdmin;

import lombok.Data;

@Data
public class GeneralStatsResponseDTO {
    private Long totalUsers;
    /** Los demás van en null hasta que existan los roles */
    private Long totalOrganizers;
    private Long totalStaff;
    private Long totalAssistants;
    private Long totalJudges;
    private Long totalParticipants;
}