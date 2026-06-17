package com.auth.services;

import com.auth.models.dtos.dashboardAdmin.GeneralStatsResponseDTO;
import com.auth.models.dtos.dashboardAdmin.MonthlyGrowthResponseDTO;
import com.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStatsService {
    private final UserRepository userRepository;

    public GeneralStatsResponseDTO getStats() {
        GeneralStatsResponseDTO stats = new GeneralStatsResponseDTO();
        stats.setTotalUsers(userRepository.count());
        // Los demás campos quedan en null hasta que existan los roles
        return stats;
    }

    public List<MonthlyGrowthResponseDTO> getMonthlyGrowth(String startDate, String endDate) {
        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime end   = endDate   != null
                ? LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        return userRepository.countUsersByMonth(start, end)
                .stream()
                .map(row -> new MonthlyGrowthResponseDTO(
                        (String) row[0],
                        (Long)   row[1]
                ))
                .toList();
    }
}