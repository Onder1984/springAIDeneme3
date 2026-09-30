package com.c2.ew.web;

import com.c2.ew.agent.dto.TacticalEmissionDetailDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.dto.TacticalQueryRequest;
import com.c2.ew.agent.dto.TacticalResponseDto;
import com.c2.ew.agent.service.TacticalAgentService;
import com.c2.ew.agent.tools.RadarEmissionTools;
import com.c2.ew.domain.model.PagedResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/tactical", "/api/tactical"})
@CrossOrigin(origins = "*")
public class TacticalController {

    private final TacticalAgentService agentService;
    private final RadarEmissionTools emissionTools;

    public TacticalController(TacticalAgentService agentService, RadarEmissionTools emissionTools) {
        this.agentService = agentService;
        this.emissionTools = emissionTools;
    }

    @PostMapping("/analyze")
    public ResponseEntity<TacticalResponseDto> analyze(@RequestBody(required = false) TacticalQueryRequest request) {
        return ResponseEntity.ok(agentService.processTacticalQuery(request));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMacroStats() {
        return ResponseEntity.ok(emissionTools.getTacticalMacroStats());
    }

    @GetMapping("/threats")
    public ResponseEntity<List<TacticalEmissionSummaryDto>> getPriorityThreats(@RequestParam(required = false, defaultValue = "15") Integer limit) {
        return ResponseEntity.ok(emissionTools.queryPriorityThreats(limit));
    }

    @GetMapping("/emissions")
    public ResponseEntity<PagedResult<TacticalEmissionSummaryDto>> getEmissionsPaged(
        @RequestParam(required = false) String teshisKimlik,
        @RequestParam(required = false) String veriKaynagi,
        @RequestParam(required = false) String radarAdi,
        @RequestParam(required = false) String hss,
        @RequestParam(required = false) Boolean taciz,
        @RequestParam(required = false) String etUygulamaDurumu,
        @RequestParam(required = false) String platformOrtami,
        @RequestParam(required = false) Double minFrekansMhz,
        @RequestParam(required = false) Double maxFrekansMhz,
        @RequestParam(required = false) String frekansTipi,
        @RequestParam(required = false) Double minPriMicroSec,
        @RequestParam(required = false) Double maxPriMicroSec,
        @RequestParam(required = false) Double minPwMicroSec,
        @RequestParam(required = false) Double maxPwMicroSec,
        @RequestParam(required = false) Double minAtpMicroSec,
        @RequestParam(required = false) Double maxAtpMicroSec,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "15") Integer size
    ) {
        return ResponseEntity.ok(emissionTools.queryEmissionsPaged(
            teshisKimlik, veriKaynagi, radarAdi, hss, taciz, etUygulamaDurumu, platformOrtami,
            minFrekansMhz, maxFrekansMhz, frekansTipi,
            minPriMicroSec, maxPriMicroSec,
            minPwMicroSec, maxPwMicroSec,
            minAtpMicroSec, maxAtpMicroSec,
            page, size
        ));
    }

    @GetMapping("/emissions/{id}")
    public ResponseEntity<TacticalEmissionDetailDto> getEmissionDetails(@PathVariable String id) {
        return ResponseEntity.ok(emissionTools.getEmissionDetails(id));
    }

    @GetMapping("/time-frequency")
    public ResponseEntity<List<Map<String, Object>>> getTimeFrequency(
        @RequestParam(required = false) String teshisKimlik,
        @RequestParam(required = false) String veriKaynagi,
        @RequestParam(required = false) String radarAdi,
        @RequestParam(required = false, defaultValue = "300") Integer limit
    ) {
        return ResponseEntity.ok(emissionTools.getTimeFrequencyDistribution(teshisKimlik, veriKaynagi, radarAdi, limit));
    }

    @GetMapping("/et-harassment")
    public ResponseEntity<List<TacticalEmissionSummaryDto>> getEtAndHarassment() {
        return ResponseEntity.ok(emissionTools.queryElectronicAttackAndHarassment());
    }
}
