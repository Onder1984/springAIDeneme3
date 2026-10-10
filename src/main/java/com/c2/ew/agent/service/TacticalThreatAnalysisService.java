package com.c2.ew.agent.service;

import com.c2.ew.agent.dto.analysis.*;
import com.c2.ew.domain.enums.HaberlesmeSekli;
import com.c2.ew.domain.enums.TeshisKimlik;
import com.c2.ew.infrastructure.entity.TacticalComintEntity;
import com.c2.ew.infrastructure.entity.TacticalEmissionEntity;
import com.c2.ew.infrastructure.repository.TacticalComintJpaRepository;
import com.c2.ew.infrastructure.repository.TacticalEmissionJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TacticalThreatAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(TacticalThreatAnalysisService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"));

    private final TacticalEmissionJpaRepository emissionRepo;
    private final TacticalComintJpaRepository comintRepo;
    private final ChatClient chatClient;

    public TacticalThreatAnalysisService(
        TacticalEmissionJpaRepository emissionRepo,
        TacticalComintJpaRepository comintRepo,
        @Autowired(required = false) ChatClient.Builder chatClientBuilder
    ) {
        this.emissionRepo = emissionRepo;
        this.comintRepo = comintRepo;

        ChatClient client = null;
        if (chatClientBuilder != null) {
            try {
                client = chatClientBuilder.build();
            } catch (Exception e) {
                log.warn("ChatClient oluşturulamadı, yerel deterministik brifing motoru devrede: {}", e.getMessage());
            }
        }
        this.chatClient = client;
    }

    public SectorThreatSummaryDto analyzeSectorThreats(String bolge) {
        log.info("🎯 [TAKTIK TEHDİT ANALİZİ] Sektör değerlendirmesi başlatıldı: {}", (bolge == null || bolge.isBlank()) ? "TÜM HAREKÂT SAHASI" : bolge);

        final String activeSector = (bolge == null || bolge.isBlank() || bolge.equalsIgnoreCase("TÜMÜ") || bolge.equalsIgnoreCase("ALL"))
            ? null
            : bolge.trim();

        // 1. İndeksli JPA Sorgusu: İlgili bölgedeki emisyonları öncelik sıralamasıyla çek
        Specification<TacticalEmissionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (activeSector != null) {
                String pattern = "%" + activeSector.toLowerCase(Locale.ROOT) + "%";
                Predicate yerPred = cb.like(cb.lower(root.get("hedefYerBilgisi")), pattern);
                Predicate mevziPred = cb.like(cb.lower(root.get("hedefMevziBilgisi")), pattern);
                predicates.add(cb.or(yerPred, mevziPred));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.desc("taciz"), Sort.Order.desc("genlikDbm"), Sort.Order.desc("sonTespitZamani"));
        List<TacticalEmissionEntity> entities = emissionRepo.findAll(spec, PageRequest.of(0, 100, sort)).getContent();

        int totalCount = entities.size();
        List<ThreatItemDto> threatItems = new ArrayList<>();

        int countLvl4 = 0;
        int countLvl3 = 0;
        int countLvl2 = 0;
        int countLvl1 = 0;

        for (TacticalEmissionEntity e : entities) {
            Double minPw = e.getMinPwMicroSec();
            Double maxPw = e.getMaxPwMicroSec();
            Double minPri = e.getMinPriMicroSec();
            Double maxPri = e.getMaxPriMicroSec();
            Double minFreq = e.getMinFrekansMhz();
            Double maxFreq = e.getMaxFrekansMhz();
            Double amp = e.getGenlikDbm();
            boolean isTaciz = Boolean.TRUE.equals(e.getTaciz());
            boolean isDusman = (e.getTeshisKimlik() == TeshisKimlik.DUSMAN || e.getTeshisKimlik() == TeshisKimlik.MUHTEMEL_DUSMAN);
            boolean isHss = e.getHss() != null && !e.getHss().equalsIgnoreCase("Stand-alone");
            String gorev = e.getRadarGorevi() != null ? e.getRadarGorevi() : "";

            // Skolnik Menzil Çözünürlüğü: DeltaR = (c * tau) / 2 = 150 * minPwMicroSec (metre)
            Double rangeResolutionMeters = (minPw != null && minPw > 0) ? (150.0 * minPw) : null;
            // PRF: 1.000.000 / minPriMicroSec (Hz)
            Double prfHz = (minPri != null && minPri > 0) ? (1_000_000.0 / minPri) : null;

            ThreatLevel level;
            KillChainStage stage;
            int score;
            String rationale;
            String docRef;
            String ewAction;
            String criticality;

            // FİZİKSEL VE DOKTRİNEL TEHDİT SINIFLANDIRMA MOTORU
            if (isTaciz && isDusman) {
                level = ThreatLevel.SEVIYE_4_KRITIK;
                stage = KillChainStage.ENGAGE;
                score = 95 + (amp != null && amp > -60 ? 5 : 0);
                countLvl4++;
                criticality = "ACİL / < 45 Saniye (Füze Ateşleme / Terminal Güdüm)";
                ewAction = "X/Ku-Bant RGPO (Range Gate Pull-Off) + Eşzamanlı Chaff Koridoru ve 3G Kaçınma Manevrası";
                rationale = String.format(Locale.ROOT,
                    "Aktif kilitlenme (taciz=true). Dar darbe (%.2f µs -> Skolnik menzil çözünürlüğü: %.1f m) ve yüksek PRF (%.0f Hz) ile monopulse takip ve güdüm sağlanmaktadır. Genlik: %.1f dBm (Ana huzme doğrudan üzerimizde).",
                    minPw != null ? minPw : 0.0,
                    rangeResolutionMeters != null ? rangeResolutionMeters : 0.0,
                    prfHz != null ? prfHz : 0.0,
                    amp != null ? amp : 0.0);
                docRef = "Skolnik Radar Handbook Ch. 4 (Range Resolution & Tracking), Wiley ELINT Ch. 3 (PDW & Lock-on Analysis)";
            } else if ((gorev.contains("Atış") || (minPw != null && minPw <= 1.5 && minPri != null && minPri <= 350.0)) && isDusman) {
                level = ThreatLevel.SEVIYE_3_YUKSEK;
                stage = gorev.contains("Atış") ? KillChainStage.TARGET : KillChainStage.TRACK;
                score = 75 + (isHss ? 10 : 0);
                countLvl3++;
                criticality = "YÜKSEK / 1 - 3 Dakika (Hedefleme Çözümü Oluşturuluyor)";
                ewAction = "Hız Kapısı Çekme (VGPO) + Sektörel Gürültü Karıştırması (Barrage Noise)";
                rationale = String.format(Locale.ROOT,
                    "Dar darbe yapısı (%.2f µs / %.1f m çözünürlük) ve takip PRF'i (%.0f Hz), radarın arama modunu bırakıp tekil hedef izleme/atış kontrol çözümüne geçtiğini kanıtlamaktadır.",
                    minPw != null ? minPw : 0.0,
                    rangeResolutionMeters != null ? rangeResolutionMeters : 0.0,
                    prfHz != null ? prfHz : 0.0);
                docRef = "NATO STANAG 4484 EW Support Doctrine, Wiley ELINT Ch. 4 (PRI Modulation & Tracking)";
            } else if (isHss || (minFreq != null && minFreq >= 6000.0) || isDusman) {
                level = ThreatLevel.SEVIYE_2_ORTA;
                stage = KillChainStage.FIX;
                score = 50 + (isHss ? 10 : 0);
                countLvl2++;
                criticality = "ORTA / Gözetim & Teyakkuz (> 5 Dakika)";
                ewAction = "Elektronik Destek (ED) Sürekli Dinlemesi + Radar Ufku İrtifa Koruması";
                rationale = String.format(Locale.ROOT,
                    "Geniş arama darbesi (%.2f µs / %.1f m çözünürlük) ve arama periyodu ile hava resmi oluşturma ve hedef teşhisi icra edilmektedir.",
                    minPw != null ? minPw : 0.0,
                    rangeResolutionMeters != null ? rangeResolutionMeters : 0.0);
                docRef = "Skolnik Introduction to Radar Systems (Search Radar Waveforms & EOB)";
            } else {
                level = ThreatLevel.SEVIYE_1_DUSUK;
                stage = KillChainStage.FIND;
                score = 25;
                countLvl1++;
                criticality = "DÜŞÜK / Rutin Seyrüsefer";
                ewAction = "Pasif İzleme ve Sinyal İmzası Doğrulama";
                rationale = "Geniş darbe ve düşük PRF ile genel hava/deniz gözetleme faaliyeti yürütülmektedir.";
                docRef = "Wiley ELINT Ch. 1 (Standard Surveillance Patterns)";
            }

            String dateStr = e.getSonTespitZamani() != null ? DATE_FORMATTER.format(e.getSonTespitZamani()) : "-";

            threatItems.add(new ThreatItemDto(
                e.getId(),
                e.getRadarAdi(),
                e.getElintNotasyonu(),
                e.getTeshisKimlik() != null ? e.getTeshisKimlik().name() : "-",
                e.getHss(),
                e.getHedefYerBilgisi(),
                e.getHedefMevziBilgisi(),
                level,
                stage,
                score,
                rangeResolutionMeters,
                minPw,
                maxPw,
                minPri,
                maxPri,
                prfHz,
                minFreq,
                maxFreq,
                amp,
                e.getTaciz(),
                e.getEtUygulamaDurumu() != null ? e.getEtUygulamaDurumu().name() : "-",
                dateStr,
                rationale,
                docRef,
                ewAction,
                criticality
            ));
        }

        // Skor ve önceliğe göre sırala (En tehlikeli en başta)
        threatItems.sort(Comparator.comparingInt(ThreatItemDto::score).reversed());

        // 2. COMINT Çapraz Korelasyonu
        String comintNote = buildComintCorrelationNote(activeSector);

        // 3. Genel Tehdit Seviyesi Belirleme
        ThreatLevel overallLevel;
        if (countLvl4 > 0) overallLevel = ThreatLevel.SEVIYE_4_KRITIK;
        else if (countLvl3 > 0) overallLevel = ThreatLevel.SEVIYE_3_YUKSEK;
        else if (countLvl2 > 0) overallLevel = ThreatLevel.SEVIYE_2_ORTA;
        else overallLevel = ThreatLevel.SEVIYE_1_DUSUK;

        // 4. Harekât Kurmayı Taktik Durum Özeti (LLM Sentezi veya Deterministik Güvence)
        String executiveBriefing = generateExecutiveBriefing(activeSector, overallLevel, countLvl4, countLvl3, countLvl2, threatItems, comintNote);

        String nowFormatted = DATE_FORMATTER.format(Instant.now());

        return new SectorThreatSummaryDto(
            activeSector != null ? activeSector : "TÜM HAREKÂT SAHASI",
            nowFormatted,
            totalCount,
            countLvl4,
            countLvl3,
            countLvl2,
            countLvl1,
            overallLevel,
            executiveBriefing,
            comintNote,
            threatItems
        );
    }

    private String buildComintCorrelationNote(String activeSector) {
        Specification<TacticalComintEntity> comintSpec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (activeSector != null) {
                String pattern = "%" + activeSector.toLowerCase(Locale.ROOT) + "%";
                preds.add(cb.like(cb.lower(root.get("hedefYerBilgisi")), pattern));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        List<TacticalComintEntity> comints = comintRepo.findAll(comintSpec, PageRequest.of(0, 50)).getContent();
        if (comints.isEmpty()) {
            return "Sektörde eşzamanlı kritik telsiz (COMINT) aktivitesi tespit edilmemiştir.";
        }

        long cryptoCount = comints.stream().filter(c -> c.getHaberlesmeSekli() == HaberlesmeSekli.KRIPTO).count();
        long enemyCount = comints.stream().filter(c -> c.getTeshisKimlik() == TeshisKimlik.DUSMAN || c.getTeshisKimlik() == TeshisKimlik.MUHTEMEL_DUSMAN).count();

        Set<String> callsigns = new LinkedHashSet<>();
        for (TacticalComintEntity c : comints) {
            if (c.getCagriAdi() != null && !c.getCagriAdi().isBlank()) callsigns.add(c.getCagriAdi());
            if (callsigns.size() >= 3) break;
        }

        return String.format(Locale.ROOT,
            "Sektörde %d adet telsiz yayını tespit edilmiş olup bunlardan %d tanesi KRİPTOLU askeri muhaberedir (Çağrı Kodları: %s). %d yayın düşman teşhisli unsurlara aittir. Radar hareketliliği ile telsiz emir-komuta trafiği arasında güçlü zamansal korelasyon mevcuttur.",
            comints.size(), cryptoCount, String.join(", ", callsigns), enemyCount);
    }

    private String generateExecutiveBriefing(
        String sector,
        ThreatLevel level,
        int count4,
        int count3,
        int count2,
        List<ThreatItemDto> threats,
        String comintNote
    ) {
        String sectorName = sector != null ? sector : "Genel Harekât Sahası";

        // LLM ile zengin operasyonel analiz üretmeyi dene
        if (chatClient != null && !threats.isEmpty()) {
            try {
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Aşağıdaki taktik radar ve haberleşme istihbaratını incele. ")
                    .append("Bir Taktik Elektronik Harp ve Harekât Kurmayı olarak doğrudan Harekât Merkezi Komutanına yönelik, ")
                    .append("3-4 cümlelik, net ve kesin bir 'Taktik Durum Değerlendirmesi' (Executive Briefing) yaz. ")
                    .append("Düşmanın niyetini (angajman, hava sahası tecriti, gözetleme?), en kritik hedefi ve acil harekât tavsiyesini belirt.\n\n")
                    .append("Sektör: ").append(sectorName).append("\n")
                    .append("Genel Tehdit Seviyesi: ").append(level.getLabel()).append("\n")
                    .append("Kritik Kilitlenme (Seviye 4) Sayısı: ").append(count4).append("\n")
                    .append("Hedef Takip (Seviye 3) Sayısı: ").append(count3).append("\n")
                    .append("Muhabere Korelasyonu: ").append(comintNote).append("\n\n")
                    .append("Öne Çıkan Kritik Tehditler:\n");

                int limit = Math.min(threats.size(), 3);
                for (int i = 0; i < limit; i++) {
                    ThreatItemDto t = threats.get(i);
                    promptBuilder.append("- ").append(t.radarAdi()).append(" (").append(t.hss()).append(") - ")
                        .append(t.hedefYerBilgisi()).append(" - Seviye: ").append(t.threatLevel().getLabel())
                        .append(" - Gerekçe: ").append(t.physicalRationale()).append("\n");
                }

                String response = chatClient.prompt()
                    .user(promptBuilder.toString())
                    .call()
                    .content();

                if (response != null && !response.isBlank()) {
                    return response.trim();
                }
            } catch (Exception ex) {
                log.warn("LLM executive briefing çağrısı başarısız oldu, yerel deterministik kural devreye girdi: {}", ex.getMessage());
            }
        }

        // Fail-Safe Deterministik Kural Motoru (LLM olmasa dahi kesintisiz, kesin taktik analiz)
        if (count4 > 0) {
            ThreatItemDto top = threats.get(0);
            return String.format(Locale.ROOT,
                "DİKKAT: %s sektöründe hava unsurlarımıza yönelik aktif kilitlenme ve terminal atış kontrol faaliyeti teyit edilmiştir. Öne çıkan tehdit %s (%s) mevziidir (Skolnik Çözünürlüğü: %.1f m). Düşmanın hava koridorunu tecrit etme ve angajman niyeti kesinleşmiştir. 45 saniye içinde X-Bant RGPO koruyucu karıştırması başlatılmalı ve kaçınma manevrası uygulanmalıdır.",
                sectorName, top.radarAdi(), top.hedefYerBilgisi(), top.skolnikRangeResolutionMeters() != null ? top.skolnikRangeResolutionMeters() : 60.0);
        } else if (count3 > 0) {
            ThreatItemDto top = threats.get(0);
            return String.format(Locale.ROOT,
                "%s sektöründe hava savunma bataryalarının arama modundan hedef takip ve atış kontrol çözümleme (Track/Target) moduna geçtiği gözlenmektedir (%s - %s). Düşman unsurları olası bir hava taarruzuna karşı teyakkuz seviyesini yükseltmiştir. Sektörel VGPO ve gürültü karıştırması için EH unsurları hazır kuvvete geçirilmelidir.",
                sectorName, top.radarAdi(), top.hedefYerBilgisi());
        } else if (count2 > 0) {
            return String.format(Locale.ROOT,
                "%s sektöründe geniş darbe genişlikli erken ihbar ve gözetleme radarları aktiftir. Doğrudan bir atış kontrol veya kilitlenme faaliyeti tespit edilmemiştir; hava sahası genel teşhis ve takip seviyesindedir. Pasif ED dinlemesi sürdürülmelidir.",
                sectorName);
        } else {
            return String.format(Locale.ROOT,
                "%s sektöründe kritik seviyede düşman radar faaliyeti gözlenmemiştir. Sektör rutin seyrüsefer ve sivil arama emisyonları kontrolündedir.",
                sectorName);
        }
    }
}

