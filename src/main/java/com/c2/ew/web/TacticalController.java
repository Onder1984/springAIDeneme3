package com.c2.ew.web;

import com.c2.ew.agent.dto.CallsignPairStatsDto;
import com.c2.ew.agent.dto.TablePageResponse;
import com.c2.ew.agent.dto.TacticalComintDetailDto;
import com.c2.ew.agent.dto.TacticalComintSummaryDto;
import com.c2.ew.agent.dto.TacticalDistributionDto;
import com.c2.ew.agent.dto.TacticalEmissionDetailDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.dto.TacticalQueryRequest;
import com.c2.ew.agent.dto.TacticalResponseDto;
import com.c2.ew.agent.dto.TacticalWaveformTimelineDto;
import com.c2.ew.agent.service.TacticalAgentService;
import com.c2.ew.agent.service.TacticalDistributionService;
import com.c2.ew.agent.service.TacticalWaveformTimelineService;
import com.c2.ew.agent.tools.ComintEmissionTools;
import com.c2.ew.agent.tools.RadarEmissionTools;
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
    private final ComintEmissionTools comintTools;
    private final TacticalDistributionService distributionService;
    private final TacticalWaveformTimelineService timelineService;

    public TacticalController(
        TacticalAgentService agentService,
        RadarEmissionTools emissionTools,
        ComintEmissionTools comintTools,
        TacticalDistributionService distributionService,
        TacticalWaveformTimelineService timelineService
    ) {
        this.agentService = agentService;
        this.emissionTools = emissionTools;
        this.comintTools = comintTools;
        this.distributionService = distributionService;
        this.timelineService = timelineService;
    }

    /**
     * Taktik AI Sohbet & Doğal Dil Karar Destek Terminali
     */
    @PostMapping("/analyze")
    public ResponseEntity<TacticalResponseDto> analyze(@RequestBody(required = false) TacticalQueryRequest request) {
        return ResponseEntity.ok(agentService.processTacticalQuery(request));
    }

    /**
     * Radar / ELINT Makro Durumsal Farkındalık İstatistikleri (Header KPI Sayaçları)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMacroStats() {
        return ResponseEntity.ok(emissionTools.getTacticalMacroStats());
    }

    /**
     * Muhabere / COMINT Makro Durumsal Farkındalık İstatistikleri (Header KPI Sayaçları)
     */
    @GetMapping("/comint/stats")
    public ResponseEntity<Map<String, Object>> getComintMacroStats() {
        return ResponseEntity.ok(comintTools.getComintMacroStats());
    }

    /**
     * Telsiz Çağrı Adı İletişim Matrisi (ECharts Isı Haritası & Ağ Analizi)
     */
    @GetMapping("/comint/callsign-matrix")
    public ResponseEntity<List<CallsignPairStatsDto>> getCallsignMatrix(
        @RequestParam(required = false) String cagriAdi,
        @RequestParam(required = false) String karsiCagriAdi,
        @RequestParam(required = false) String baslangicZamani,
        @RequestParam(required = false) String bitisZamani,
        @RequestParam(defaultValue = "15") Integer limit
    ) {
        return ResponseEntity.ok(comintTools.getCallsignPairInteractionStats(
            cagriAdi, karsiCagriAdi, baslangicZamani, bitisZamani, limit
        ));
    }

    /**
     * Taktik Kategorik Dağılım Analizi (Pie / Donut Chart)
     */
    @GetMapping({"/analytics/distribution", "/distribution"})
    public ResponseEntity<TacticalDistributionDto> getDistribution(
        @RequestParam(defaultValue = "RADAR") String source,
        @RequestParam(defaultValue = "TESHIS") String criterion,
        @RequestParam(required = false) String timeRange,
        @RequestParam(defaultValue = "8") Integer limit
    ) {
        return ResponseEntity.ok(distributionService.getDistribution(source, criterion, timeRange, limit));
    }

    /**
     * Taktik Dalga Formu & Zamansal Değişim Analizi (Radar: Spot No, COMINT: Çağrı Adı)
     */
    @GetMapping({"/analytics/waveform-timeline", "/waveform-timeline"})
    public ResponseEntity<TacticalWaveformTimelineDto> getWaveformTimeline(
        @RequestParam(defaultValue = "RADAR") String source,
        @RequestParam(required = false) String targetKey,
        @RequestParam(required = false) String timeRange,
        @RequestParam(defaultValue = "200") Integer limit
    ) {
        return ResponseEntity.ok(timelineService.getTimeline(source, targetKey, timeRange, limit));
    }

    /**
     * Taktik Dalga Formu Takip Edilebilir Hedefler Listesi (En aktif Spot No ve Çağrı Adları)
     */
    @GetMapping({"/analytics/waveform-targets", "/waveform-targets"})
    public ResponseEntity<List<TacticalWaveformTimelineDto.AvailableTarget>> getWaveformTargets(
        @RequestParam(defaultValue = "RADAR") String source
    ) {
        boolean isRadar = source == null || !source.equalsIgnoreCase("COMINT");
        return ResponseEntity.ok(timelineService.getAvailableTargets(isRadar));
    }

    /**
     * Radar Emisyonları Sayfalı ve Filtreli Sorgulama (Ant Design Table Server-Side Pagination)
     */
    @GetMapping("/emissions")
    public ResponseEntity<TablePageResponse<TacticalEmissionSummaryDto>> getEmissions(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String teshis,
        @RequestParam(required = false) String gorev,
        @RequestParam(required = false) String platform,
        @RequestParam(required = false) Boolean taciz,
        @RequestParam(required = false) String et,
        @RequestParam(required = false) Double minFrekans,
        @RequestParam(required = false) Double maxFrekans,
        @RequestParam(required = false) Double minPri,
        @RequestParam(required = false) Double maxPri,
        @RequestParam(required = false) Double minPw,
        @RequestParam(required = false) Double maxPw,
        @RequestParam(required = false) Double minGenlik,
        @RequestParam(required = false) Double maxGenlik,
        @RequestParam(required = false) String bolge,
        @RequestParam(required = false) String hss,
        @RequestParam(required = false) String sortField,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) String kayitTipi
    ) {
        return ResponseEntity.ok(emissionTools.getEmissionsPage(
            page, size, search, teshis, gorev, platform, taciz, et,
            minFrekans, maxFrekans, minPri, maxPri, minPw, maxPw, minGenlik, maxGenlik, bolge, hss,
            sortField, sortOrder, kayitTipi
        ));
    }

    /**
     * Tüm Radar Emisyonları (İstemci tarafı tam liste talepleri için)
     */
    @GetMapping("/emissions/all")
    public ResponseEntity<List<TacticalEmissionSummaryDto>> getAllEmissions() {
        return ResponseEntity.ok(emissionTools.getAllEmissions());
    }

    /**
     * Muhabere (COMINT) Yayınları Sayfalı ve Filtreli Sorgulama (Ant Design Table Server-Side Pagination)
     */
    @GetMapping({"/comints", "/comint/emissions"})
    public ResponseEntity<TablePageResponse<TacticalComintSummaryDto>> getComints(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String teshis,
        @RequestParam(required = false) String tip,
        @RequestParam(required = false) String lisan,
        @RequestParam(required = false) String haberlesme,
        @RequestParam(required = false) Boolean kripto,
        @RequestParam(required = false) Double minFrekans,
        @RequestParam(required = false) Double maxFrekans,
        @RequestParam(required = false) Double minGenlik,
        @RequestParam(required = false) Double maxGenlik,
        @RequestParam(required = false) String bolge,
        @RequestParam(required = false) String protokol,
        @RequestParam(required = false) String cagriAdi,
        @RequestParam(required = false) String karsiCagriAdi,
        @RequestParam(required = false) String sortField,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) String kayitTipi,
        @RequestParam(required = false) String zamanAraligi
    ) {
        return ResponseEntity.ok(comintTools.getComintsPage(
            page, size, search, teshis, tip, lisan, haberlesme, kripto,
            minFrekans, maxFrekans, minGenlik, maxGenlik, bolge, protokol,
            cagriAdi, karsiCagriAdi,
            sortField, sortOrder, kayitTipi, zamanAraligi
        ));
    }

    /**
     * Tüm Muhabere Yayınları (İstemci tarafı tam liste talepleri için)
     */
    @GetMapping({"/comints/all", "/comint/emissions/all"})
    public ResponseEntity<List<TacticalComintSummaryDto>> getAllComints() {
        return ResponseEntity.ok(comintTools.getAllComints());
    }

    /**
     * Radar Emisyonu 37 Parametreli Detay Analizi ([EMI-xxxx] Tıklama Modalı / Drawer)
     */
    @GetMapping("/emissions/{id}")
    public ResponseEntity<TacticalEmissionDetailDto> getEmissionDetails(@PathVariable String id) {
        return ResponseEntity.ok(emissionTools.getEmissionDetails(id));
    }

    /**
     * Muhabere (COMINT) Yayını 30 Parametreli Detay Analizi ([COM-xxxx] Tıklama Modalı / Drawer)
     */
    @GetMapping({"/comint/emissions/{id}", "/comint/{id}"})
    public ResponseEntity<TacticalComintDetailDto> getComintDetails(@PathVariable String id) {
        return ResponseEntity.ok(comintTools.getComintDetails(id));
    }
}

