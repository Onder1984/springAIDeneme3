package com.c2.ew.agent.tools;

import com.c2.ew.agent.dto.PaginationMetadata;
import com.c2.ew.agent.dto.TacticalEmissionDetailDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.domain.enums.*;
import com.c2.ew.domain.model.AtpEntry;
import com.c2.ew.domain.model.FrequencyEntry;
import com.c2.ew.domain.model.PagedResult;
import com.c2.ew.domain.model.PriEntry;
import com.c2.ew.domain.model.PwEntry;
import com.c2.ew.infrastructure.entity.TacticalEmissionEntity;
import com.c2.ew.infrastructure.repository.TacticalEmissionJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RadarEmissionTools {

    private static final Logger log = LoggerFactory.getLogger(RadarEmissionTools.class);

    private static final ThreadLocal<List<String>> CALL_LOGS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Map<String, TacticalEmissionSummaryDto>> QUERIED_EMISSIONS = ThreadLocal.withInitial(LinkedHashMap::new);

    // Oturum Bazlı Sayfalama Hafızası (Session Pagination State)
    private static final Map<String, ActivePaginationState> SESSION_STATES = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_CONVERSATION_ID = ThreadLocal.withInitial(() -> "c2-tactical-session");

    public static void setCurrentConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            CURRENT_CONVERSATION_ID.set(conversationId);
        }
    }

    public static String getCurrentConversationId() {
        return CURRENT_CONVERSATION_ID.get();
    }

    public static ActivePaginationState getActiveState() {
        return SESSION_STATES.computeIfAbsent(CURRENT_CONVERSATION_ID.get(), k -> new ActivePaginationState());
    }

    public static class ActivePaginationState {
        public String lastTeshis;
        public String lastVeriKaynagi;
        public String lastRadarAdi;
        public String lastHss;
        public Boolean lastTaciz;
        public String lastEtUygulamaDurumu;
        public String lastPlatformOrtami;
        public Double lastMinFrekansMhz;
        public Double lastMaxFrekansMhz;
        public String lastFrekansTipi;
        public Double lastMinPriMicroSec;
        public Double lastMaxPriMicroSec;
        public Double lastMinPwMicroSec;
        public Double lastMaxPwMicroSec;
        public Double lastMinAtpMicroSec;
        public Double lastMaxAtpMicroSec;

        public int lastEmissionPage = -1;
        public int emissionPageSize = 15;
        public long emissionTotalCount = 0;
        public int emissionTotalPages = 0;
        public boolean emissionHasMore = false;
    }

    public static List<String> drainCallLogs() {
        List<String> logs = new ArrayList<>(CALL_LOGS.get());
        CALL_LOGS.get().clear();
        return logs;
    }

    public static List<TacticalEmissionSummaryDto> drainQueriedEmissions() {
        List<TacticalEmissionSummaryDto> list = new ArrayList<>(QUERIED_EMISSIONS.get().values());
        QUERIED_EMISSIONS.get().clear();
        return list;
    }

    public static PaginationMetadata getPaginationMetadata() {
        ActivePaginationState state = getActiveState();
        if (state.lastEmissionPage < 0 || state.emissionTotalCount == 0) {
            return null;
        }
        return new PaginationMetadata(
            state.emissionHasMore,
            state.lastEmissionPage,
            state.emissionTotalPages,
            state.emissionTotalCount,
            state.emissionPageSize,
            "EMISSIONS",
            state.emissionHasMore ? "Sonraki sayfadaki hedefleri listelemek için 'sonraki sayfayı getir' veya 'devamını göster' diyebilirsiniz." : null
        );
    }

    private final TacticalEmissionJpaRepository emissionRepo;
    private final ObjectMapper objectMapper;

    public RadarEmissionTools(TacticalEmissionJpaRepository emissionRepo, ObjectMapper objectMapper) {
        this.emissionRepo = emissionRepo;
        this.objectMapper = objectMapper;
    }

    private String cleanParam(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        if (trimmed.isEmpty() ||
            trimmed.equalsIgnoreCase("ALL") ||
            trimmed.equalsIgnoreCase("ANY") ||
            trimmed.equalsIgnoreCase("NULL") ||
            trimmed.equalsIgnoreCase("NONE") ||
            trimmed.equalsIgnoreCase("*") ||
            trimmed.equalsIgnoreCase("TÜMÜ") ||
            trimmed.equalsIgnoreCase("HEPSİ") ||
            trimmed.equalsIgnoreCase("TUMU") ||
            trimmed.equalsIgnoreCase("HEPSI")) {
            return null;
        }
        return trimmed;
    }

    @Tool(description = "Sahadaki tüm taktik radar emisyonlarının makro özet istatistiklerini döner. Teşhis (Dost/Düşman), Veri Kaynağı (Kara/Hava/Deniz/GenKur), Platform Ortamı, Aktif Elektronik Taarruz (ET) ve Taciz sayılarını içerir.")
    public Map<String, Object> getTacticalMacroStats() {
        CALL_LOGS.get().add("Tool Çağrısı: getTacticalMacroStats() çalıştırıldı.");

        long totalCount = emissionRepo.count();
        long tacizCount = emissionRepo.countByTacizTrue();
        long etUygulaniyorCount = emissionRepo.countByEtUygulamaDurumu(EtUygulamaDurumu.UYGULANIYOR);
        long etSusturduCount = emissionRepo.countByEtUygulamaDurumu(EtUygulamaDurumu.SUSTURDU);

        Map<String, Long> teshisMap = new LinkedHashMap<>();
        for (Object[] row : emissionRepo.getTeshisDistribution()) {
            teshisMap.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Map<String, Long> veriKaynagiMap = new LinkedHashMap<>();
        for (Object[] row : emissionRepo.getVeriKaynagiDistribution()) {
            veriKaynagiMap.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Map<String, Long> platformMap = new LinkedHashMap<>();
        for (Object[] row : emissionRepo.getPlatformOrtamiDistribution()) {
            platformMap.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toplamEmisyonSayisi", totalCount);
        result.put("tacizYapanHedefSayisi", tacizCount);
        result.put("aktifEtUygulananSayisi", etUygulaniyorCount);
        result.put("susturulanHedefSayisi", etSusturduCount);
        result.put("teshisDagilimi", teshisMap);
        result.put("veriKaynagiDagilimi", veriKaynagiMap);
        result.put("platformOrtamiDagilimi", platformMap);

        return result;
    }

    @Tool(description = "En kritik öncelikli tehditleri listeler: Teşhisi DÜŞMAN veya MUHTEMEL_DÜŞMAN olan, taciz yapan veya HSS içeren en güncel ilk 15 hedefi döner.")
    public List<TacticalEmissionSummaryDto> queryPriorityThreats(Integer limit) {
        int max = (limit == null || limit <= 0) ? 15 : Math.min(limit, 30);
        CALL_LOGS.get().add("Tool Çağrısı: queryPriorityThreats(limit=" + max + ") çalıştırıldı.");

        Specification<TacticalEmissionEntity> spec = (root, query, cb) -> {
            Predicate isEnemy = root.get("teshisKimlik").in(TeshisKimlik.DUSMAN, TeshisKimlik.MUHTEMEL_DUSMAN);
            Predicate isTaciz = cb.isTrue(root.get("taciz"));
            Predicate hasHss = cb.isNotNull(root.get("hss"));
            return cb.or(isTaciz, cb.and(isEnemy, hasHss));
        };

        Page<TacticalEmissionEntity> page = emissionRepo.findAll(
            spec,
            PageRequest.of(0, max, Sort.by(Sort.Direction.DESC, "sonTespitZamani"))
        );

        List<TacticalEmissionSummaryDto> dtoList = page.getContent().stream().map(this::toSummaryDto).collect(Collectors.toList());
        dtoList.forEach(d -> QUERIED_EMISSIONS.get().put(d.id(), d));
        return dtoList;
    }

    @Tool(description = "Taktik radar emisyonlarını çoklu kriterlerle ve ARALIKLI filtrelerle (Frekans min-max, PRI min-max, PW min-max, ATP min-max, Teşhis, Veri Kaynağı, Radar Adı, HSS, Taciz, ET Durumu) sayfalı olarak sorgular.")
    public PagedResult<TacticalEmissionSummaryDto> queryEmissionsPaged(
        String teshisKimlik,
        String veriKaynagi,
        String radarAdi,
        String hss,
        Boolean taciz,
        String etUygulamaDurumu,
        String platformOrtami,
        Double minFrekansMhz,
        Double maxFrekansMhz,
        String frekansTipi,
        Double minPriMicroSec,
        Double maxPriMicroSec,
        Double minPwMicroSec,
        Double maxPwMicroSec,
        Double minAtpMicroSec,
        Double maxAtpMicroSec,
        Integer sayfaNo,
        Integer sayfaBoyutu
    ) {
        String cleanTeshis = cleanParam(teshisKimlik);
        String cleanKaynak = cleanParam(veriKaynagi);
        String cleanRadar = cleanParam(radarAdi);
        String cleanHss = cleanParam(hss);
        String cleanEt = cleanParam(etUygulamaDurumu);
        String cleanPlatform = cleanParam(platformOrtami);
        String cleanFreqType = cleanParam(frekansTipi);

        // LLM'ler ve kullanıcılar ilk sayfayı genellikle 1 olarak gönderir.
        int pageIdx = (sayfaNo == null || sayfaNo <= 0) ? 0 : (sayfaNo >= 1 ? sayfaNo - 1 : 0);
        int size = (sayfaBoyutu == null || sayfaBoyutu <= 0) ? 15 : Math.min(sayfaBoyutu, 50);

        ActivePaginationState state = getActiveState();
        state.lastTeshis = cleanTeshis;
        state.lastVeriKaynagi = cleanKaynak;
        state.lastRadarAdi = cleanRadar;
        state.lastHss = cleanHss;
        state.lastTaciz = taciz;
        state.lastEtUygulamaDurumu = cleanEt;
        state.lastPlatformOrtami = cleanPlatform;
        state.lastMinFrekansMhz = minFrekansMhz;
        state.lastMaxFrekansMhz = maxFrekansMhz;
        state.lastFrekansTipi = cleanFreqType;
        state.lastMinPriMicroSec = minPriMicroSec;
        state.lastMaxPriMicroSec = maxPriMicroSec;
        state.lastMinPwMicroSec = minPwMicroSec;
        state.lastMaxPwMicroSec = maxPwMicroSec;
        state.lastMinAtpMicroSec = minAtpMicroSec;
        state.lastMaxAtpMicroSec = maxAtpMicroSec;
        state.lastEmissionPage = pageIdx;
        state.emissionPageSize = size;

        log.info("queryEmissionsPaged called with teshis={}, freq=[{}-{}], page={}", cleanTeshis, minFrekansMhz, maxFrekansMhz, pageIdx);
        CALL_LOGS.get().add(String.format("Tool: queryEmissionsPaged(teshis=%s, kaynak=%s, freq=[%s-%s], pri=[%s-%s], taciz=%s, page=%d)",
            cleanTeshis, cleanKaynak, minFrekansMhz, maxFrekansMhz, minPriMicroSec, maxPriMicroSec, taciz, pageIdx));

        Specification<TacticalEmissionEntity> spec = createSpecification(
            cleanTeshis, cleanKaynak, cleanRadar, cleanHss, taciz, cleanEt, cleanPlatform,
            minFrekansMhz, maxFrekansMhz, cleanFreqType,
            minPriMicroSec, maxPriMicroSec,
            minPwMicroSec, maxPwMicroSec,
            minAtpMicroSec, maxAtpMicroSec
        );

        Page<TacticalEmissionEntity> pageResult = emissionRepo.findAll(
            spec,
            PageRequest.of(pageIdx, size, Sort.by(Sort.Direction.DESC, "sonTespitZamani"))
        );

        state.emissionTotalCount = pageResult.getTotalElements();
        state.emissionTotalPages = pageResult.getTotalPages();
        state.emissionHasMore = pageResult.hasNext();

        List<TacticalEmissionSummaryDto> dtos = pageResult.getContent().stream().map(this::toSummaryDto).collect(Collectors.toList());
        dtos.forEach(d -> QUERIED_EMISSIONS.get().put(d.id(), d));

        String note = String.format("Sayfa %d / %d (Toplam %d hedef)", pageIdx + 1, pageResult.getTotalPages(), pageResult.getTotalElements());
        return PagedResult.of(dtos, pageIdx, size, pageResult.getTotalElements(), note);
    }

    @Tool(description = "Kullanıcı 'sonraki sayfayı getir', 'sıradakileri göster' veya 'devamını getir' dediğinde önceki arama kriterlerini otomatik hatırlayarak bir sonraki sayfayı (page + 1) çeker.")
    public PagedResult<TacticalEmissionSummaryDto> getNextPageOfEmissions() {
        ActivePaginationState state = getActiveState();
        int nextPage = state.lastEmissionPage + 1;
        CALL_LOGS.get().add("Tool Çağrısı: getNextPageOfEmissions() -> Sayfa " + (nextPage + 1) + " isteniyor.");

        return queryEmissionsPaged(
            state.lastTeshis, state.lastVeriKaynagi, state.lastRadarAdi, state.lastHss, state.lastTaciz,
            state.lastEtUygulamaDurumu, state.lastPlatformOrtami,
            state.lastMinFrekansMhz, state.lastMaxFrekansMhz, state.lastFrekansTipi,
            state.lastMinPriMicroSec, state.lastMaxPriMicroSec,
            state.lastMinPwMicroSec, state.lastMaxPwMicroSec,
            state.lastMinAtpMicroSec, state.lastMaxAtpMicroSec,
            nextPage, state.emissionPageSize
        );
    }

    @Tool(description = "Kullanıcı 'önceki sayfaya dön' veya 'bir önceki hedefleri göster' dediğinde önceki arama kriterlerini hatırlayarak önceki sayfayı (page - 1) çeker.")
    public PagedResult<TacticalEmissionSummaryDto> getPrevPageOfEmissions() {
        ActivePaginationState state = getActiveState();
        int prevPage = Math.max(0, state.lastEmissionPage - 1);
        CALL_LOGS.get().add("Tool Çağrısı: getPrevPageOfEmissions() -> Sayfa " + (prevPage + 1) + " isteniyor.");

        return queryEmissionsPaged(
            state.lastTeshis, state.lastVeriKaynagi, state.lastRadarAdi, state.lastHss, state.lastTaciz,
            state.lastEtUygulamaDurumu, state.lastPlatformOrtami,
            state.lastMinFrekansMhz, state.lastMaxFrekansMhz, state.lastFrekansTipi,
            state.lastMinPriMicroSec, state.lastMaxPriMicroSec,
            state.lastMinPwMicroSec, state.lastMaxPwMicroSec,
            state.lastMinAtpMicroSec, state.lastMaxAtpMicroSec,
            prevPage, state.emissionPageSize
        );
    }

    @Tool(description = "Elektronik Taarruz (ET) uygulanan, susturulan veya hudut tacizi yapan hedeflerin özel operasyonel durum raporunu getirir.")
    public List<TacticalEmissionSummaryDto> queryElectronicAttackAndHarassment() {
        CALL_LOGS.get().add("Tool: queryElectronicAttackAndHarassment() çalıştırıldı.");

        Specification<TacticalEmissionEntity> spec = (root, query, cb) -> {
            Predicate etActive = cb.equal(root.get("etUygulamaDurumu"), EtUygulamaDurumu.UYGULANIYOR);
            Predicate etSusturdu = cb.equal(root.get("etUygulamaDurumu"), EtUygulamaDurumu.SUSTURDU);
            Predicate isTaciz = cb.isTrue(root.get("taciz"));
            return cb.or(etActive, etSusturdu, isTaciz);
        };

        List<TacticalEmissionEntity> list = emissionRepo.findAll(
            spec,
            Sort.by(Sort.Direction.DESC, "sonTespitZamani")
        );

        List<TacticalEmissionSummaryDto> dtos = list.stream().map(this::toSummaryDto).collect(Collectors.toList());
        dtos.forEach(d -> QUERIED_EMISSIONS.get().put(d.id(), d));
        return dtos;
    }

    @Tool(description = "Belirli bir taktik emisyonun (emissionId) çoklu Frekans Listesini, PRI/PW listelerini, Anten Tarama Deseni (ATP) periyotlarını, operatör ve onay notlarını içeren derin teknik detaylarını döner.")
    public TacticalEmissionDetailDto getEmissionDetails(String emissionId) {
        CALL_LOGS.get().add("Tool: getEmissionDetails(emissionId=" + emissionId + ") çalıştırıldı.");

        TacticalEmissionEntity e = emissionRepo.findById(emissionId)
            .orElseThrow(() -> new IllegalArgumentException("Emisyon bulunamadı: " + emissionId));

        List<FrequencyEntry> freqList = Collections.emptyList();
        List<PriEntry> priList = Collections.emptyList();
        List<PwEntry> pwList = Collections.emptyList();
        List<AtpEntry> atpList = Collections.emptyList();

        try {
            if (e.getFrekansListesiJson() != null) {
                freqList = objectMapper.readValue(e.getFrekansListesiJson(), new TypeReference<>() {});
            }
            if (e.getPriListesiJson() != null) {
                priList = objectMapper.readValue(e.getPriListesiJson(), new TypeReference<>() {});
            }
            if (e.getPwListesiJson() != null) {
                pwList = objectMapper.readValue(e.getPwListesiJson(), new TypeReference<>() {});
            }
            if (e.getAtpListesiJson() != null) {
                atpList = objectMapper.readValue(e.getAtpListesiJson(), new TypeReference<>() {});
            }
        } catch (Exception ex) {
            log.error("JSON parse hatası: {}", ex.getMessage());
        }

        return new TacticalEmissionDetailDto(
            e.getId(),
            e.getVeriKaynagi() != null ? e.getVeriKaynagi().name() : null,
            e.getKuvvetSiraNo(),
            e.getUstBirlikRef(),
            e.getEhUnsuru(),
            e.getYon(),
            e.getEhUnsuruEnlem(),
            e.getEhUnsuruBoylam(),
            e.getEhUnsuruIrtifa(),
            e.getYayinEnlem(),
            e.getYayinBoylam(),
            e.getYayinSemiMajorMeters(),
            e.getYayinSemiMinorMeters(),
            e.getYayinOrientationDegrees(),
            e.getHedefYerBilgisi(),
            e.getHedefMevziBilgisi(),
            e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : null,
            e.getElintNotasyonu(),
            e.getHedefKaynagi() != null ? e.getHedefKaynagi().name() : null,
            e.getPolarizasyon() != null ? e.getPolarizasyon().name() : null,
            e.getSpotNo(),
            e.getPulseCw() != null ? e.getPulseCw().name() : null,
            e.getRadarGorevi(),
            e.getRadarAdi(),
            e.getPlatformOrtami() != null ? e.getPlatformOrtami().name() : null,
            e.getPlatformTipi() != null ? e.getPlatformTipi().name() : null,
            e.getGenlikDbm(),
            e.getModulasyon() != null ? e.getModulasyon().name() : null,
            e.getEtUygulamaDurumu() != null ? e.getEtUygulamaDurumu().name() : null,
            e.getIlkTespitZamani(),
            e.getSonTespitZamani(),
            e.getSureSn(),
            e.getHss(),
            e.getTaciz(),
            e.getRadarKesitAlani(),
            e.getRadarIzNumarasi(),
            e.getUcakKuyrukNumarasi(),
            e.getMinFrekansMhz(),
            e.getMaxFrekansMhz(),
            e.getMinPriMicroSec(),
            e.getMaxPriMicroSec(),
            e.getMinPwMicroSec(),
            e.getMaxPwMicroSec(),
            e.getMinAtpMicroSec(),
            e.getMaxAtpMicroSec(),
            freqList,
            priList,
            pwList,
            atpList,
            e.getOperatorNotu(),
            e.getOnaylayanKullanici(),
            e.getOnayNotu()
        );
    }

    @Tool(description = "Zaman-Frekans spektral şelale (Scatter/Waterfall) grafiği için ölçüm noktalarını döner. Her nokta: dakika cinsinden zaman, frekans (MHz), teşhis, radar adı ve PRI bilgisini içerir.")
    public List<Map<String, Object>> getTimeFrequencyDistribution(String teshisKimlik, String veriKaynagi, String radarAdi, Integer limit) {
        String cleanTeshis = cleanParam(teshisKimlik);
        String cleanKaynak = cleanParam(veriKaynagi);
        String cleanRadar = cleanParam(radarAdi);
        int max = (limit == null || limit <= 0) ? 250 : Math.min(limit, 500);

        CALL_LOGS.get().add(String.format("Tool: getTimeFrequencyDistribution(teshis=%s, kaynak=%s, radar=%s, limit=%d)", cleanTeshis, cleanKaynak, cleanRadar, max));

        Specification<TacticalEmissionEntity> spec = createSpecification(
            cleanTeshis, cleanKaynak, cleanRadar, null, null, null, null,
            null, null, null, null, null, null, null, null, null
        );

        List<TacticalEmissionEntity> list = emissionRepo.findAll(
            spec,
            PageRequest.of(0, max, Sort.by(Sort.Direction.DESC, "sonTespitZamani"))
        ).getContent();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

        List<Map<String, Object>> points = new ArrayList<>();
        for (TacticalEmissionEntity e : list) {
            if (e.getSonTespitZamani() == null || e.getMinFrekansMhz() == null) continue;

            String timeStr = timeFmt.format(e.getSonTespitZamani());
            String[] parts = timeStr.split(":");
            int minutesFromMidnight = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("emissionId", e.getId());
            p.put("timeStr", timeStr);
            p.put("timeMinutes", minutesFromMidnight);
            p.put("frequencyMhz", e.getMinFrekansMhz());
            p.put("maxFrequencyMhz", e.getMaxFrekansMhz());
            p.put("radarAdi", e.getRadarAdi());
            p.put("teshisKimlik", e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : "BILINMEYEN");
            p.put("taciz", e.getTaciz());
            p.put("etUygulamaDurumu", e.getEtUygulamaDurumu() != null ? e.getEtUygulamaDurumu().name() : "YOK");
            p.put("priMicroSec", e.getMinPriMicroSec());
            p.put("pwMicroSec", e.getMinPwMicroSec());
            points.add(p);
        }

        return points;
    }

    private TeshisKimlik normalizeTeshis(String val) {
        if (val == null) return null;
        String v = val.toUpperCase().replace("İ", "I").replace("Ü", "U").replace("Ş", "S");
        if (v.contains("MUHTEMEL") && v.contains("DUS")) return TeshisKimlik.MUHTEMEL_DUSMAN;
        if (v.contains("MUHTEMEL") && v.contains("DOST")) return TeshisKimlik.MUHTEMEL_DOST;
        if (v.contains("DUSMAN") || v.contains("TEHDIT")) return TeshisKimlik.DUSMAN;
        if (v.contains("DOST")) return TeshisKimlik.DOST;
        if (v.contains("BILIN")) return TeshisKimlik.BILINMEYEN;
        if (v.contains("TARAF")) return TeshisKimlik.TARAFSIZ;
        try { return TeshisKimlik.valueOf(v); } catch (Exception ignored) { return null; }
    }

    private VeriKaynagi normalizeVeriKaynagi(String val) {
        if (val == null) return null;
        String v = val.toUpperCase().replace("İ", "I");
        if (v.contains("DENIZ")) return VeriKaynagi.DENIZ;
        if (v.contains("KARA")) return VeriKaynagi.KARA;
        if (v.contains("HAVA")) return VeriKaynagi.HAVA;
        if (v.contains("GEN")) return VeriKaynagi.GEN_KUR;
        try { return VeriKaynagi.valueOf(v); } catch (Exception ignored) { return null; }
    }

    private PlatformOrtami normalizePlatformOrtami(String val) {
        if (val == null) return null;
        String v = val.toUpperCase().replace("İ", "I").replace("Ü", "U");
        if (v.contains("HAVA")) return PlatformOrtami.HAVA;
        if (v.contains("KARA")) return PlatformOrtami.KARA;
        if (v.contains("DENIZ") || v.contains("SU_USTU") || v.contains("SU USTU")) return PlatformOrtami.SU_USTU;
        if (v.contains("SU_ALTI") || v.contains("SU ALTI")) return PlatformOrtami.SU_ALTI;
        try { return PlatformOrtami.valueOf(v); } catch (Exception ignored) { return null; }
    }

    private EtUygulamaDurumu normalizeEtDurumu(String val) {
        if (val == null) return null;
        String v = val.toUpperCase().replace("İ", "I").replace("Ş", "S");
        if (v.contains("SUSTUR")) return EtUygulamaDurumu.SUSTURDU;
        if (v.contains("UYGULAN") || v.contains("AKTIF")) return EtUygulamaDurumu.UYGULANIYOR;
        if (v.contains("YOK") || v.contains("UYGULANMIYOR")) return EtUygulamaDurumu.UYGULANMIYOR;
        try { return EtUygulamaDurumu.valueOf(v); } catch (Exception ignored) { return null; }
    }

    private Specification<TacticalEmissionEntity> createSpecification(
        String teshisKimlik, String veriKaynagi, String radarAdi, String hss, Boolean taciz,
        String etUygulamaDurumu, String platformOrtami,
        Double minFreq, Double maxFreq, String freqType,
        Double minPri, Double maxPri,
        Double minPw, Double maxPw,
        Double minAtp, Double maxAtp
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            TeshisKimlik tk = normalizeTeshis(teshisKimlik);
            if (tk != null) {
                predicates.add(cb.equal(root.get("teshisKimlik"), tk));
            }

            VeriKaynagi vk = normalizeVeriKaynagi(veriKaynagi);
            if (vk != null) {
                predicates.add(cb.equal(root.get("veriKaynagi"), vk));
            }

            PlatformOrtami po = normalizePlatformOrtami(platformOrtami);
            if (po != null) {
                predicates.add(cb.equal(root.get("platformOrtami"), po));
            }

            EtUygulamaDurumu et = normalizeEtDurumu(etUygulamaDurumu);
            if (et != null) {
                predicates.add(cb.equal(root.get("etUygulamaDurumu"), et));
            }

            if (radarAdi != null) {
                predicates.add(cb.like(cb.lower(root.get("radarAdi")), "%" + radarAdi.toLowerCase() + "%"));
            }

            if (hss != null) {
                predicates.add(cb.like(cb.lower(root.get("hss")), "%" + hss.toLowerCase() + "%"));
            }

            if (taciz != null) {
                predicates.add(cb.equal(root.get("taciz"), taciz));
            }

            if (etUygulamaDurumu != null) {
                try {
                    predicates.add(cb.equal(root.get("etUygulamaDurumu"), EtUygulamaDurumu.valueOf(etUygulamaDurumu.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            if (platformOrtami != null) {
                try {
                    predicates.add(cb.equal(root.get("platformOrtami"), PlatformOrtami.valueOf(platformOrtami.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            // Dummy / Halüsinasyon sınır temizliği
            Double effectiveMinFreq = (minFreq != null && minFreq > 0.0) ? minFreq : null;
            Double effectiveMaxFreq = (maxFreq != null && maxFreq < 900000.0) ? maxFreq : null;
            Double effectiveMinPri = (minPri != null && minPri > 0.0) ? minPri : null;
            Double effectiveMaxPri = (maxPri != null && maxPri < 900000.0) ? maxPri : null;
            Double effectiveMinPw = (minPw != null && minPw > 0.0) ? minPw : null;
            Double effectiveMaxPw = (maxPw != null && maxPw < 900000.0) ? maxPw : null;
            Double effectiveMinAtp = (minAtp != null && minAtp > 0.0) ? minAtp : null;
            Double effectiveMaxAtp = (maxAtp != null && maxAtp < 90000000.0) ? maxAtp : null;

            // 📡 Frekans Aralığı Kesişim Mantığı: target.min <= query.max AND target.max >= query.min
            if (effectiveMinFreq != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxFrekansMhz"), effectiveMinFreq));
            }
            if (effectiveMaxFreq != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minFrekansMhz"), effectiveMaxFreq));
            }
            if (freqType != null) {
                predicates.add(cb.like(cb.lower(root.get("frekansListesiJson")), "%" + freqType.toLowerCase() + "%"));
            }

            // ⏱️ PRI Aralığı Kesişim Mantığı
            if (effectiveMinPri != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPriMicroSec"), effectiveMinPri));
            }
            if (effectiveMaxPri != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPriMicroSec"), effectiveMaxPri));
            }

            // 📏 PW Aralığı Kesişim Mantığı
            if (effectiveMinPw != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPwMicroSec"), effectiveMinPw));
            }
            if (effectiveMaxPw != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPwMicroSec"), effectiveMaxPw));
            }

            // 🔄 ATP Aralığı Kesişim Mantığı
            if (effectiveMinAtp != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxAtpMicroSec"), effectiveMinAtp));
            }
            if (effectiveMaxAtp != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minAtpMicroSec"), effectiveMaxAtp));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TacticalEmissionSummaryDto toSummaryDto(TacticalEmissionEntity e) {
        return new TacticalEmissionSummaryDto(
            e.getId(),
            e.getVeriKaynagi() != null ? e.getVeriKaynagi().name() : null,
            e.getKuvvetSiraNo(),
            e.getEhUnsuru(),
            e.getYon(),
            e.getEhUnsuruEnlem(),
            e.getEhUnsuruBoylam(),
            e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : null,
            e.getRadarAdi(),
            e.getRadarGorevi(),
            e.getHss(),
            e.getTaciz(),
            e.getEtUygulamaDurumu() != null ? e.getEtUygulamaDurumu().name() : null,
            e.getPlatformOrtami() != null ? e.getPlatformOrtami().name() : null,
            e.getPlatformTipi() != null ? e.getPlatformTipi().name() : null,
            e.getHedefYerBilgisi(),
            e.getHedefMevziBilgisi(),
            e.getMinFrekansMhz(),
            e.getMaxFrekansMhz(),
            e.getMinPriMicroSec(),
            e.getMaxPriMicroSec(),
            e.getMinPwMicroSec(),
            e.getMaxPwMicroSec(),
            e.getMinAtpMicroSec(),
            e.getMaxAtpMicroSec(),
            e.getSonTespitZamani(),
            e.getYayinEnlem(),
            e.getYayinBoylam(),
            e.getYayinSemiMajorMeters(),
            e.getYayinSemiMinorMeters(),
            e.getYayinOrientationDegrees(),
            e.getSpotNo(),
            e.getElintNotasyonu(),
            e.getPolarizasyon() != null ? e.getPolarizasyon().name() : null,
            e.getPulseCw() != null ? e.getPulseCw().name() : null,
            e.getModulasyon() != null ? e.getModulasyon().name() : null,
            e.getGenlikDbm(),
            e.getSureSn(),
            e.getUcakKuyrukNumarasi(),
            e.getOperatorNotu(),
            e.getOnaylayanKullanici(),
            e.getOnayNotu()
        );
    }
}





