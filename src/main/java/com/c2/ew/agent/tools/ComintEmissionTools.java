package com.c2.ew.agent.tools;

import com.c2.ew.agent.dto.PaginationMetadata;
import com.c2.ew.agent.dto.TacticalComintDetailDto;
import com.c2.ew.agent.dto.TacticalComintSummaryDto;
import com.c2.ew.domain.enums.*;
import com.c2.ew.domain.model.MuhabereFrekansEntry;
import com.c2.ew.domain.model.PagedResult;
import com.c2.ew.infrastructure.entity.TacticalComintEntity;
import com.c2.ew.infrastructure.repository.TacticalComintJpaRepository;
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
public class ComintEmissionTools {

    private static final Logger log = LoggerFactory.getLogger(ComintEmissionTools.class);

    private static final ThreadLocal<List<String>> COMINT_CALL_LOGS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Map<String, TacticalComintSummaryDto>> QUERIED_COMINTS = ThreadLocal.withInitial(LinkedHashMap::new);

    private static final Map<String, ActiveComintPaginationState> COMINT_SESSION_STATES = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_CONVERSATION_ID = ThreadLocal.withInitial(() -> "c2-tactical-session");

    public static void setCurrentConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            CURRENT_CONVERSATION_ID.set(conversationId);
        }
    }

    public static String getCurrentConversationId() {
        return CURRENT_CONVERSATION_ID.get();
    }

    public static ActiveComintPaginationState getActiveState() {
        return COMINT_SESSION_STATES.computeIfAbsent(CURRENT_CONVERSATION_ID.get(), k -> new ActiveComintPaginationState());
    }

    public static class ActiveComintPaginationState {
        public String lastTeshis;
        public String lastVeriKaynagi;
        public String lastLisan;
        public String lastTip;
        public String lastHaberlesmeSekli;
        public String lastProtokol;
        public String lastModulasyon;
        public String lastCagriAdi;
        public Boolean lastMti;
        public Double lastMinFrekansMhz;
        public Double lastMaxFrekansMhz;
        public Double lastMinGenlikDbm;
        public Double lastMaxGenlikDbm;

        public int lastComintPage = -1;
        public int comintPageSize = 10;
        public long comintTotalCount = 0;
        public int comintTotalPages = 0;
        public boolean comintHasMore = false;
    }

    public static List<String> drainCallLogs() {
        List<String> logs = new ArrayList<>(COMINT_CALL_LOGS.get());
        COMINT_CALL_LOGS.get().clear();
        return logs;
    }

    public static List<TacticalComintSummaryDto> drainQueriedComints() {
        List<TacticalComintSummaryDto> list = new ArrayList<>(QUERIED_COMINTS.get().values());
        QUERIED_COMINTS.get().clear();
        return list;
    }

    public static PaginationMetadata getPaginationMetadata() {
        ActiveComintPaginationState state = getActiveState();
        if (state.lastComintPage < 0 || state.comintTotalCount == 0) {
            return null;
        }
        return new PaginationMetadata(
            state.comintHasMore,
            state.lastComintPage,
            state.comintTotalPages,
            state.comintTotalCount,
            state.comintPageSize,
            "COMINT_EMISSIONS",
            state.comintHasMore ? "Sonraki sayfadaki muhabere hedeflerini listelemek için 'sonraki sayfayı getir' veya 'devamını göster' diyebilirsiniz." : null
        );
    }

    private final TacticalComintJpaRepository comintRepo;
    private final ObjectMapper objectMapper;

    public ComintEmissionTools(TacticalComintJpaRepository comintRepo, ObjectMapper objectMapper) {
        this.comintRepo = comintRepo;
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
            trimmed.equalsIgnoreCase("HEPSI") ||
            trimmed.equalsIgnoreCase("FARKETMEZ")) {
            return null;
        }
        return trimmed;
    }

    private TeshisKimlik normalizeTeshis(String raw) {
        String clean = cleanParam(raw);
        if (clean == null) return null;
        String upper = clean.toUpperCase(Locale.ENGLISH);

        if (clean.equalsIgnoreCase("D\u00DC\u015EMAN") || upper.contains("DUSMAN") || upper.contains("ENEMY") || upper.contains("D\u00DC\u015E")) {
            if (upper.contains("MUHTEMEL") || upper.contains("PROBABLE") || upper.contains("SUSPECT")) {
                return TeshisKimlik.MUHTEMEL_DUSMAN;
            }
            return TeshisKimlik.DUSMAN;
        }
        if (clean.equalsIgnoreCase("DOST") || upper.contains("DOST") || upper.contains("FRIEND")) {
            if (upper.contains("MUHTEMEL") || upper.contains("PROBABLE")) {
                return TeshisKimlik.MUHTEMEL_DOST;
            }
            return TeshisKimlik.DOST;
        }
        if (upper.contains("TARAFSIZ") || upper.contains("NEUTRAL")) {
            return TeshisKimlik.TARAFSIZ;
        }
        if (upper.contains("BILINMEYEN") || upper.contains("UNKNOWN") || clean.equalsIgnoreCase("B\u0130L\u0130NMEYEN")) {
            return TeshisKimlik.BILINMEYEN;
        }
        try {
            return TeshisKimlik.valueOf(upper);
        } catch (Exception e) {
            return null;
        }
    }

    private MuhabereBantTipi normalizeTip(String raw) {
        String clean = cleanParam(raw);
        if (clean == null) return null;
        String upper = clean.toUpperCase(Locale.ENGLISH);
        if (upper.contains("HF") && !upper.contains("VHF")) return MuhabereBantTipi.HF;
        if (upper.contains("VHF")) return MuhabereBantTipi.VHF;
        try {
            return MuhabereBantTipi.valueOf(upper);
        } catch (Exception e) {
            return null;
        }
    }

    private HaberlesmeSekli normalizeHaberlesmeSekli(String raw) {
        String clean = cleanParam(raw);
        if (clean == null) return null;
        String upper = clean.toUpperCase(Locale.ENGLISH);
        if (upper.contains("KRIPTO") || upper.contains("SIFRE") || upper.contains("\u015E\u0130FRE")) return HaberlesmeSekli.KRIPTO;
        if (upper.contains("DARBE") || upper.contains("BURST") || upper.contains("PATLAMA")) return HaberlesmeSekli.DARBE_PATLAMA;
        if (upper.contains("GAYDA")) return HaberlesmeSekli.KARISTIRMA_GAYDA;
        if (upper.contains("GURULTU") || upper.contains("G\u00DCR\u00DCLT\u00DC") || upper.contains("NOISE")) return HaberlesmeSekli.KARISTIRMA_GURULTU;
        if (upper.contains("VERI") || upper.contains("DATA") || upper.contains("VER\u0130")) return HaberlesmeSekli.VERI;
        if (upper.contains("SES") || upper.contains("VOICE") || upper.contains("AUDIO")) return HaberlesmeSekli.SES;
        try {
            return HaberlesmeSekli.valueOf(upper);
        } catch (Exception e) {
            return null;
        }
    }

    @Tool(description = "Taktik sahada tespit edilen muhabere (COMINT / CESM telsiz ve haberleşme) yayınlarının makro özetini döner. Toplam telsiz yayını sayısı, HF/VHF dağılımı, lisanlar, haberleşme türleri (kriptolu, darbe patlama, karıştırma vb.) ve teşhis dağılımını verir.")
    public Map<String, Object> getComintMacroStats() {
        COMINT_CALL_LOGS.get().add("Tool Çağrısı: getComintMacroStats() çalıştırıldı.");
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalCount = comintRepo.count();
        stats.put("toplamMuhabereKaydi", totalCount);
        stats.put("dusmanMuhabereSayisi", comintRepo.countByTeshisKimlik(TeshisKimlik.DUSMAN));
        stats.put("kriptoluYayinSayisi", comintRepo.countByHaberlesmeSekli(HaberlesmeSekli.KRIPTO));
        stats.put("darbePatlamaSayisi", comintRepo.countByHaberlesmeSekli(HaberlesmeSekli.DARBE_PATLAMA));
        stats.put("karistirmaGaydaSayisi", comintRepo.countByHaberlesmeSekli(HaberlesmeSekli.KARISTIRMA_GAYDA));
        stats.put("karistirmaGurultuSayisi", comintRepo.countByHaberlesmeSekli(HaberlesmeSekli.KARISTIRMA_GURULTU));
        stats.put("hfBantSayisi", comintRepo.countByTip(MuhabereBantTipi.HF));
        stats.put("vhfBantSayisi", comintRepo.countByTip(MuhabereBantTipi.VHF));

        stats.put("teshisDagilimi", mapDistribution(comintRepo.getTeshisDistribution()));
        stats.put("haberlesmeSekliDagilimi", mapDistribution(comintRepo.getHaberlesmeSekliDistribution()));
        stats.put("tipDagilimi", mapDistribution(comintRepo.getTipDistribution()));
        stats.put("lisanDagilimi", mapDistribution(comintRepo.getLisanDistribution()));
        stats.put("veriKaynagiDagilimi", mapDistribution(comintRepo.getVeriKaynagiDistribution()));

        return stats;
    }

    @Tool(description = "Taktik sahada tespit edilen öncelikli ve kritik düşman muhabere yayınlarını listeler (Düşman, Kriptolu, Darbe Patlama / Burst veya Karıştırma uygulayan kritik telsiz ağları).")
    public List<TacticalComintSummaryDto> queryPriorityComintThreats(Integer limit) {
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 15;
        COMINT_CALL_LOGS.get().add(String.format("Tool Çağrısı: queryPriorityComintThreats(limit=%d) çalıştırıldı.", effectiveLimit));

        Specification<TacticalComintEntity> spec = (root, query, cb) -> {
            Predicate isDusman = cb.equal(root.get("teshisKimlik"), TeshisKimlik.DUSMAN);
            Predicate isMuhtemelDusman = cb.equal(root.get("teshisKimlik"), TeshisKimlik.MUHTEMEL_DUSMAN);
            Predicate isKripto = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.KRIPTO);
            Predicate isDarbe = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.DARBE_PATLAMA);
            Predicate isGayda = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.KARISTIRMA_GAYDA);
            Predicate isGurultu = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.KARISTIRMA_GURULTU);

            return cb.or(isDusman, isMuhtemelDusman, isKripto, isDarbe, isGayda, isGurultu);
        };

        Page<TacticalComintEntity> page = comintRepo.findAll(spec, PageRequest.of(0, effectiveLimit, Sort.by(Sort.Direction.DESC, "sonTespitZamani")));
        List<TacticalComintSummaryDto> dtos = page.getContent().stream().map(this::toSummaryDto).collect(Collectors.toList());

        for (TacticalComintSummaryDto dto : dtos) {
            QUERIED_COMINTS.get().put(dto.id(), dto);
        }
        return dtos;
    }

    @Tool(description = "Muhabere (telsiz/haberleşme) yayınlarını sayfalı ve detaylı filtrelerle sorgular. Frekans aralığı (minFrekansMhz - maxFrekansMhz aralık kesişimi), teşhis (DÜŞMAN, DOST vb.), lisan (Türkçe, Rusça, Arapça...), tip (HF, VHF), haberleşme şekli (SES, KRIPTO, DARBE_PATLAMA, GAYDA vb.), protokol, çağrı adı ve sayfalama destekler.")
    public PagedResult<TacticalComintSummaryDto> queryComintEmissionsPaged(
        String teshisKimlik,
        String veriKaynagi,
        String lisan,
        String tip,
        String haberlesmeSekli,
        String protokol,
        String modulasyon,
        String cagriAdi,
        Boolean mti,
        Double minFrekansMhz,
        Double maxFrekansMhz,
        Double minGenlikDbm,
        Double maxGenlikDbm,
        Integer sayfaNo,
        Integer sayfaBoyutu
    ) {
        String cleanTeshis = cleanParam(teshisKimlik);
        String cleanKaynak = cleanParam(veriKaynagi);
        String cleanLisan = cleanParam(lisan);
        String cleanTip = cleanParam(tip);
        String cleanHaberlesme = cleanParam(haberlesmeSekli);
        String cleanProtokol = cleanParam(protokol);
        String cleanModulasyon = cleanParam(modulasyon);
        String cleanCagri = cleanParam(cagriAdi);

        // Dummy LLM aralıklarını filtrele
        Double effMinFreq = (minFrekansMhz != null && minFrekansMhz <= 0.0 && (maxFrekansMhz == null || maxFrekansMhz >= 100000.0)) ? null : minFrekansMhz;
        Double effMaxFreq = (maxFrekansMhz != null && maxFrekansMhz >= 100000.0 && (minFrekansMhz == null || minFrekansMhz <= 0.0)) ? null : maxFrekansMhz;

        int pageIdx = (sayfaNo == null || sayfaNo <= 0) ? 0 : sayfaNo - 1;
        int pageSize = (sayfaBoyutu != null && sayfaBoyutu > 0) ? Math.min(sayfaBoyutu, 50) : 10;

        ActiveComintPaginationState state = getActiveState();
        state.lastTeshis = cleanTeshis;
        state.lastVeriKaynagi = cleanKaynak;
        state.lastLisan = cleanLisan;
        state.lastTip = cleanTip;
        state.lastHaberlesmeSekli = cleanHaberlesme;
        state.lastProtokol = cleanProtokol;
        state.lastModulasyon = cleanModulasyon;
        state.lastCagriAdi = cleanCagri;
        state.lastMti = mti;
        state.lastMinFrekansMhz = effMinFreq;
        state.lastMaxFrekansMhz = effMaxFreq;
        state.lastMinGenlikDbm = minGenlikDbm;
        state.lastMaxGenlikDbm = maxGenlikDbm;
        state.lastComintPage = pageIdx;
        state.comintPageSize = pageSize;

        log.info("queryComintEmissionsPaged called with teshis={}, lisan={}, tip={}, freq=[{}-{}], page={}",
            cleanTeshis, cleanLisan, cleanTip, effMinFreq, effMaxFreq, pageIdx);

        COMINT_CALL_LOGS.get().add(String.format("Tool: queryComintEmissionsPaged(teshis=%s, lisan=%s, tip=%s, haberlesme=%s, freq=[%s-%s], page=%d)",
            cleanTeshis, cleanLisan, cleanTip, cleanHaberlesme, effMinFreq, effMaxFreq, pageIdx));

        Specification<TacticalComintEntity> spec = createComintSpecification(
            cleanTeshis, cleanKaynak, cleanLisan, cleanTip, cleanHaberlesme, cleanProtokol, cleanModulasyon, cleanCagri, mti,
            effMinFreq, effMaxFreq, minGenlikDbm, maxGenlikDbm
        );

        Page<TacticalComintEntity> resultPage = comintRepo.findAll(spec, PageRequest.of(pageIdx, pageSize, Sort.by(Sort.Direction.DESC, "sonTespitZamani")));

        state.comintTotalCount = resultPage.getTotalElements();
        state.comintTotalPages = resultPage.getTotalPages();
        state.comintHasMore = resultPage.hasNext();

        List<TacticalComintSummaryDto> dtos = resultPage.getContent().stream().map(this::toSummaryDto).collect(Collectors.toList());

        for (TacticalComintSummaryDto dto : dtos) {
            QUERIED_COMINTS.get().put(dto.id(), dto);
        }

        String note = String.format("Sayfa %d / %d (Toplam %d muhabere yayını)", pageIdx + 1, resultPage.getTotalPages(), resultPage.getTotalElements());
        return PagedResult.of(dtos, pageIdx, pageSize, resultPage.getTotalElements(), note);
    }

    @Tool(description = "Operatör 'sonraki sayfayı ver', 'bana sonraki sayfayı da ver', 'devamını getir' dediğinde bir önceki sorgu filtrelerini (frekans, lisan, tip, teşhis vb.) hatırlayarak sonraki sayfayı çeker.")
    public PagedResult<TacticalComintSummaryDto> getNextPageOfComintEmissions() {
        ActiveComintPaginationState state = getActiveState();
        int targetPage = (state.lastComintPage < 0) ? 1 : state.lastComintPage + 2; // 1-indexed

        COMINT_CALL_LOGS.get().add(String.format("Tool Çağrısı: getNextPageOfComintEmissions() çalıştırıldı -> Sayfa: %d", targetPage));
        return queryComintEmissionsPaged(
            state.lastTeshis,
            state.lastVeriKaynagi,
            state.lastLisan,
            state.lastTip,
            state.lastHaberlesmeSekli,
            state.lastProtokol,
            state.lastModulasyon,
            state.lastCagriAdi,
            state.lastMti,
            state.lastMinFrekansMhz,
            state.lastMaxFrekansMhz,
            state.lastMinGenlikDbm,
            state.lastMaxGenlikDbm,
            targetPage,
            state.comintPageSize
        );
    }

    @Tool(description = "Operatör 'önceki sayfaya dön', 'geri git' dediğinde bir önceki sayfayı çeker.")
    public PagedResult<TacticalComintSummaryDto> getPrevPageOfComintEmissions() {
        ActiveComintPaginationState state = getActiveState();
        int targetPage = Math.max(1, (state.lastComintPage < 0) ? 1 : state.lastComintPage); // 1-indexed

        COMINT_CALL_LOGS.get().add(String.format("Tool Çağrısı: getPrevPageOfComintEmissions() çalıştırıldı -> Sayfa: %d", targetPage));
        return queryComintEmissionsPaged(
            state.lastTeshis,
            state.lastVeriKaynagi,
            state.lastLisan,
            state.lastTip,
            state.lastHaberlesmeSekli,
            state.lastProtokol,
            state.lastModulasyon,
            state.lastCagriAdi,
            state.lastMti,
            state.lastMinFrekansMhz,
            state.lastMaxFrekansMhz,
            state.lastMinGenlikDbm,
            state.lastMaxGenlikDbm,
            targetPage,
            state.comintPageSize
        );
    }

    @Tool(description = "Telsiz çağrı adı (Callsign) bazlı link ve haberleşme ağı analizi yapar. Verilen çağrı adının konuştuğu karşı çağrı adlarını (karsiCagriAdi) ve tüm ilişkili telsiz yayınlarını getirir.")
    public List<TacticalComintSummaryDto> queryComintCallsignNetwork(String cagriAdi) {
        String clean = cleanParam(cagriAdi);
        if (clean == null) return List.of();
        COMINT_CALL_LOGS.get().add(String.format("Tool Çağrısı: queryComintCallsignNetwork(cagriAdi=%s) çalıştırıldı.", clean));

        List<TacticalComintEntity> list = comintRepo.findByCagriAdiContainingIgnoreCaseOrKarsiCagriAdiContainingIgnoreCase(clean, clean);
        List<TacticalComintSummaryDto> dtos = list.stream().map(this::toSummaryDto).collect(Collectors.toList());

        for (TacticalComintSummaryDto dto : dtos) {
            QUERIED_COMINTS.get().put(dto.id(), dto);
        }
        return dtos;
    }

    @Tool(description = "Tekil bir muhabere yayınının (COM-XXXX) 30 parametresinin tamamını (frekans listesi, çağrı adları, kripto, operatör notu, onaylayan kullanıcı vb.) döner.")
    public TacticalComintDetailDto getComintDetails(String comintId) {
        String cleanId = cleanParam(comintId);
        if (cleanId == null) return null;
        COMINT_CALL_LOGS.get().add(String.format("Tool Çağrısı: getComintDetails(comintId=%s) çalıştırıldı.", cleanId));

        return comintRepo.findById(cleanId).map(this::toDetailDto).orElse(null);
    }

    private Specification<TacticalComintEntity> createComintSpecification(
        String teshis, String kaynak, String lisan, String tip, String haberlesme, String protokol, String modulasyon, String cagri, Boolean mti,
        Double minFreq, Double maxFreq, Double minGenlik, Double maxGenlik
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (teshis != null) {
                TeshisKimlik normTeshis = normalizeTeshis(teshis);
                if (normTeshis != null) {
                    predicates.add(cb.equal(root.get("teshisKimlik"), normTeshis));
                }
            }

            if (kaynak != null) {
                try {
                    predicates.add(cb.equal(root.get("veriKaynagi"), VeriKaynagi.valueOf(kaynak.toUpperCase())));
                } catch (Exception ignored) {}
            }

            if (lisan != null) {
                predicates.add(cb.like(cb.lower(root.get("lisan")), "%" + lisan.toLowerCase(Locale.ENGLISH) + "%"));
            }

            if (tip != null) {
                MuhabereBantTipi normTip = normalizeTip(tip);
                if (normTip != null) {
                    predicates.add(cb.equal(root.get("tip"), normTip));
                }
            }

            if (haberlesme != null) {
                HaberlesmeSekli normHaberlesme = normalizeHaberlesmeSekli(haberlesme);
                if (normHaberlesme != null) {
                    predicates.add(cb.equal(root.get("haberlesmeSekli"), normHaberlesme));
                }
            }

            if (protokol != null) {
                try {
                    predicates.add(cb.equal(root.get("protokol"), MuhabereProtokol.valueOf(protokol.toUpperCase())));
                } catch (Exception ignored) {}
            }

            if (modulasyon != null) {
                try {
                    predicates.add(cb.equal(root.get("modulasyon"), MuhabereModulasyon.valueOf(modulasyon.toUpperCase())));
                } catch (Exception ignored) {}
            }

            if (cagri != null) {
                Predicate byCagri = cb.like(cb.lower(root.get("cagriAdi")), "%" + cagri.toLowerCase() + "%");
                Predicate byKarsi = cb.like(cb.lower(root.get("karsiCagriAdi")), "%" + cagri.toLowerCase() + "%");
                predicates.add(cb.or(byCagri, byKarsi));
            }

            if (mti != null) {
                predicates.add(cb.equal(root.get("mti"), mti));
            }

            // Interval Overlap (Aralık Kesişimi) Sorguları:
            // Hedefin [minFrekansMhz, maxFrekansMhz] aralığı kullanıcının aradığı [minFreq, maxFreq] ile kesişmeli
            if (minFreq != null && maxFreq != null) {
                predicates.add(cb.and(
                    cb.lessThanOrEqualTo(root.get("minFrekansMhz"), maxFreq),
                    cb.greaterThanOrEqualTo(root.get("maxFrekansMhz"), minFreq)
                ));
            } else if (minFreq != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxFrekansMhz"), minFreq));
            } else if (maxFreq != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minFrekansMhz"), maxFreq));
            }

            if (minGenlik != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("genlikDbm"), minGenlik));
            }
            if (maxGenlik != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("genlikDbm"), maxGenlik));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TacticalComintSummaryDto toSummaryDto(TacticalComintEntity e) {
        return new TacticalComintSummaryDto(
            e.getId(),
            e.getVeriKaynagi() != null ? e.getVeriKaynagi().name() : null,
            e.getKuvvetSiraNo(),
            e.getGoreviIcraEdenUstBirlik(),
            e.getGoreviIcraEdenEhUnsuru(),
            e.getYon(),
            e.getEhUnsuruEnlem(),
            e.getEhUnsuruBoylam(),
            e.getYayinEnlem(),
            e.getYayinBoylam(),
            e.getYayinSemiMajorMeters(),
            e.getYayinSemiMinorMeters(),
            e.getYayinOrientationDegrees(),
            e.getHedefYerBilgisi(),
            e.getHedefKaynagi() != null ? e.getHedefKaynagi().name() : null,
            e.getLisan(),
            e.getProtokol() != null ? e.getProtokol().name() : null,
            e.getBantGenisligiHz(),
            e.getModulasyon() != null ? e.getModulasyon().name() : null,
            e.getMti(),
            e.getTip() != null ? e.getTip().name() : null,
            e.getGenlikDbm(),
            e.getHaberlesmeSekli() != null ? e.getHaberlesmeSekli().name() : null,
            e.getCalismaSekli() != null ? e.getCalismaSekli().name() : null,
            e.getAltEsikSeviyesiDbm(),
            e.getCagriAdi(),
            e.getKarsiCagriAdi(),
            e.getSonTespitZamani(),
            e.getSureSn(),
            e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : null,
            e.getMinFrekansMhz(),
            e.getMaxFrekansMhz(),
            e.getOperatorNotu(),
            e.getOnaylayanKullanici(),
            e.getOnayNotu()
        );
    }

    private TacticalComintDetailDto toDetailDto(TacticalComintEntity e) {
        List<MuhabereFrekansEntry> frekansList = Collections.emptyList();
        try {
            if (e.getFrekansListesiJson() != null && !e.getFrekansListesiJson().isBlank()) {
                frekansList = objectMapper.readValue(e.getFrekansListesiJson(), new TypeReference<List<MuhabereFrekansEntry>>() {});
            }
        } catch (Exception ex) {
            log.warn("Frekans JSON parse hatası [{}]: {}", e.getId(), ex.getMessage());
        }

        return new TacticalComintDetailDto(
            e.getId(),
            e.getVeriKaynagi() != null ? e.getVeriKaynagi().name() : null,
            e.getKuvvetSiraNo(),
            e.getGoreviIcraEdenUstBirlik(),
            e.getGoreviIcraEdenEhUnsuru(),
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
            e.getHedefKaynagi() != null ? e.getHedefKaynagi().name() : null,
            e.getVeriGirisiYapanBirlik(),
            e.getLisan(),
            e.getProtokol() != null ? e.getProtokol().name() : null,
            e.getBantGenisligiHz(),
            e.getModulasyon() != null ? e.getModulasyon().name() : null,
            e.getMti(),
            e.getTip() != null ? e.getTip().name() : null,
            e.getGenlikDbm(),
            e.getHaberlesmeSekli() != null ? e.getHaberlesmeSekli().name() : null,
            e.getCalismaSekli() != null ? e.getCalismaSekli().name() : null,
            e.getAltEsikSeviyesiDbm(),
            e.getCagriAdi(),
            e.getKarsiCagriAdi(),
            e.getIlkTespitZamani(),
            e.getSonTespitZamani(),
            e.getSureSn(),
            e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : null,
            e.getMinFrekansMhz(),
            e.getMaxFrekansMhz(),
            frekansList,
            e.getOperatorNotu(),
            e.getOnaylayanKullanici(),
            e.getOnayNotu()
        );
    }

    private Map<String, Long> mapDistribution(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] r : rows) {
                if (r[0] != null) {
                    map.put(r[0].toString(), ((Number) r[1]).longValue());
                }
            }
        }
        return map;
    }
}

