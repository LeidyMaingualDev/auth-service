package com.auth.models.dtos.dashboardAdmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyGrowthResponseDTO {
    private String month;
    private Long newUsers;
}