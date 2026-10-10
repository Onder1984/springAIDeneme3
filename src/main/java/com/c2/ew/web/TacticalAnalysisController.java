package com.c2.ew.web;

import com.c2.ew.agent.dto.analysis.SectorThreatSummaryDto;
import com.c2.ew.agent.service.TacticalThreatAnalysisService;
import com.c2.ew.infrastructure.seeder.MassDataSeederService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/tactical/analysis", "/api/tactical/analysis"})
@CrossOrigin(origins = "*")
public class TacticalAnalysisController {

    private final TacticalThreatAnalysisService threatAnalysisService;
    private final MassDataSeederService massDataSeederService;

    public TacticalAnalysisController(
        TacticalThreatAnalysisService threatAnalysisService,
        MassDataSeederService massDataSeederService
    ) {
        this.threatAnalysisService = threatAnalysisService;
        this.massDataSeederService = massDataSeederService;
    }

    /**
     * Harekât Sahası ve Sektör Tehdit/Niyet Analizi (Kill-Chain F2T2EA & Skolnik/Wiley Analizi)
     */
    @GetMapping("/threat-assessment")
    public ResponseEntity<SectorThreatSummaryDto> getThreatAssessment(
        @RequestParam(required = false) String bolge
    ) {
        SectorThreatSummaryDto result = threatAnalysisService.analyzeSectorThreats(bolge);
        return ResponseEntity.ok(result);
    }

    /**
     * Büyük Ölçekli Taktik Veri Üretici (Stress Test & Büyük Veri Seeder)
     */
    @PostMapping("/seed-massive")
    public ResponseEntity<Map<String, Object>> seedMassive(
        @RequestParam(defaultValue = "200000") int radarCount,
        @RequestParam(defaultValue = "200000") int comintCount
    ) {
        Map<String, Object> result = massDataSeederService.seedMassiveData(radarCount, comintCount);
        return ResponseEntity.ok(result);
    }
}
