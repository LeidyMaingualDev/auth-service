package com.auth.controllers;

import com.auth.models.dtos.dashboardAdmin.GeneralStatsResponseDTO;
import com.auth.models.dtos.dashboardAdmin.MonthlyGrowthResponseDTO;
import com.auth.services.UserStatsService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    /**
     * RF18 - Retorna las estadíticas generales del sistema incluyendo
     * totales de usuarios, organizadores, persdonal, asistentes, jurados,
     * participantes y eventos. 
     *
     * @Return estadísticas generales del sistema
     */
    @GetMapping("/stats")
    public ResponseEntity<GeneralStatsResponseDTO> getGeneralStats(){
        return ResponseEntity.ok(userStatsService.getStats());
    }


    /**
    * RF21 y RF21.1 - Retornar el crecimiento mensual de usuarios registrados
    * identifica el mes con mayopr crecimiento
    * @param startDate
    * @param endDate
    * @return lista de meses con nuevos usuarios y mes pico
    */
    @GetMapping("/monthly-growth")
    public ResponseEntity<List<MonthlyGrowthResponseDTO>> getMonthlyGrowth(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(userStatsService.getMonthlyGrowth(startDate, endDate));
    }

}