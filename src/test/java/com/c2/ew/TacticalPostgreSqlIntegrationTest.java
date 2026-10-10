package com.c2.ew;

import com.c2.ew.agent.dto.TacticalComintDetailDto;
import com.c2.ew.agent.dto.TacticalComintSummaryDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.dto.TacticalQueryResult;
import com.c2.ew.agent.tools.ComintEmissionTools;
import com.c2.ew.agent.tools.RadarEmissionTools;
import com.c2.ew.infrastructure.entity.TacticalComintEntity;
import com.c2.ew.infrastructure.repository.TacticalComintJpaRepository;
import com.c2.ew.infrastructure.repository.TacticalEmissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TacticalPostgreSqlIntegrationTest {

    @Autowired
    private TacticalEmissionJpaRepository emissionRepo;

    @Autowired
    private RadarEmissionTools emissionTools;

    @Autowired
    private TacticalComintJpaRepository comintRepo;

    @Autowired
    private ComintEmissionTools comintTools;

    @Test
    @DisplayName("PostgreSQL veritabanında 160 adet 37 parametreli Taktik Emisyon kalıcı yüklenmiş olmalıdır")
    void testDataSeedingAndPersistence() {
        long count = emissionRepo.count();
        assertTrue(count >= 150, "En az 150 adet Taktik Emisyon olmalıdır, bulunan: " + count);
    }

    @Test
    @DisplayName("Makro İstatistikler (Teşhis, Veri Kaynağı, ET, Taciz) anında hesaplanmalıdır")
    void testMacroStatistics() {
        Map<String, Object> stats = emissionTools.getTacticalMacroStats();
        assertNotNull(stats);
        assertTrue((Long) stats.get("toplamEmisyonSayisi") >= 150);
        assertTrue((Long) stats.get("tacizYapanHedefSayisi") > 0);
    }

    @Test
    @DisplayName("Frekans Aralığı (Interval Overlap) sorgulaması doğru çalışmalıdır")
    void testFrequencyRangeOverlapQuery() {
        TacticalQueryResult<TacticalEmissionSummaryDto> xBand = emissionTools.queryEmissions(
            null, null, null, null, null, null, null,
            8000.0, 12000.0, null,
            null, null, null, null, null, null,
            1, 20
        );

        assertNotNull(xBand);
        assertFalse(xBand.kayitlar().isEmpty(), "8000-12000 MHz aralığında hedef bulunmalıdır.");
        for (TacticalEmissionSummaryDto dto : xBand.kayitlar()) {
            assertTrue(dto.minFrekansMhz() <= 12000.0 && dto.maxFrekansMhz() >= 8000.0,
                "Hedef aralık kesişim kuralına uymalıdır: " + dto.id());
        }
    }

    @Test
    @DisplayName("COMINT (Muhabere) 120 adet telsiz yayını PostgreSQL'e kaydedilmiş olmalıdır")
    void testComintSeedingAndPersistence() {
        long count = comintRepo.count();
        assertTrue(count >= 100, "En az 100 adet Taktik Muhabere kaydı olmalıdır, bulunan: " + count);
    }

    @Test
    @DisplayName("COMINT Makro İstatistikleri (Kripto, Darbe Patlama, Karıştırma, HF/VHF) hesaplanmalıdır")
    void testComintMacroStats() {
        Map<String, Object> stats = comintTools.getComintMacroStats();
        assertNotNull(stats);
        assertTrue((Long) stats.get("toplamMuhabereKaydi") >= 100);
        assertTrue((Long) stats.get("hfBantSayisi") > 0);
        assertTrue((Long) stats.get("vhfBantSayisi") > 0);
    }

    @Test
    @DisplayName("COMINT Frekans Aralığı ve Çağrı Ağı Analizi doğru çalışmalıdır")
    void testComintCallsignAndRange() {
        List<TacticalComintSummaryDto> volgaNet = comintTools.queryComintCallsignNetwork("VOLGA");
        assertNotNull(volgaNet);
        assertFalse(volgaNet.isEmpty(), "VOLGA telsiz ağı hedefleri bulunmalıdır.");

        TacticalQueryResult<TacticalComintSummaryDto> vhfResult = comintTools.queryComintEmissions(
            null, null, null, "VHF", null, null, null, null, null,
            30.0, 250.0, null, null, 1, 10
        );
        assertNotNull(vhfResult);
        assertFalse(vhfResult.kayitlar().isEmpty(), "VHF bandında muhabere yayını bulunmalıdır.");
    }

    @Test
    @DisplayName("COMINT 30 parametreli tekil detay doğru getirilmelidir")
    void testComintSingleDetail() {
        List<TacticalComintEntity> list = comintRepo.findAll();
        assertFalse(list.isEmpty());
        String sampleId = list.get(0).getId();

        TacticalComintDetailDto detail = comintTools.getComintDetails(sampleId);
        assertNotNull(detail);
        assertEquals(sampleId, detail.id());
        assertNotNull(detail.cagriAdi());
        assertNotNull(detail.lisan());
        assertNotNull(detail.frekansListesi());
    }

    @Test
    @DisplayName("Radar Esnek Aralık, Bölge ve Zaman Aralığı sorgulaması doğru çalışmalıdır")
    void testRadarFlexibleRangeAndRegionQuery() {
        TacticalQueryResult<TacticalEmissionSummaryDto> result = emissionTools.queryEmissions(
            null, null, null, null, null, "Ege", null, null, null,
            null, null, null, null, null, null, null, null, null,
            -80.0, 0.0, 1.0, 1000.0, "SON_48_SAAT", null,
            20
        );
        assertNotNull(result);
        for (TacticalEmissionSummaryDto dto : result.kayitlar()) {
            boolean inEge = (dto.hedefYerBilgisi() != null && dto.hedefYerBilgisi().toLowerCase().contains("ege")) ||
                            (dto.hedefMevziBilgisi() != null && dto.hedefMevziBilgisi().toLowerCase().contains("ege"));
            assertTrue(inEge, "Hedef Ege bölgesinde olmalıdır: " + dto.id());
        }
    }

    @Test
    @DisplayName("COMINT Esnek Aralık, Bölge ve Zaman Aralığı sorgulaması doğru çalışmalıdır")
    void testComintFlexibleRangeAndRegionQuery() {
        TacticalQueryResult<TacticalComintSummaryDto> result = comintTools.queryComintEmissions(
            null, null, null, null, null, null, null, null, "Akdeniz", null,
            null, null, -100.0, 0.0, 1.0, 1000.0, "SON_48_SAAT", null,
            20
        );
        assertNotNull(result);
        for (TacticalComintSummaryDto dto : result.kayitlar()) {
            assertTrue(dto.hedefYerBilgisi() != null && dto.hedefYerBilgisi().toLowerCase().contains("akdeniz"),
                "Muhabere hedefi Akdeniz bölgesinde olmalıdır: " + dto.id());
        }
    }

    @Test
    @DisplayName("Tüm Radar ve COMINT kayıtları Tablo için eksiksiz getirilebilmelidir")
    void testGetAllRecordsForTables() {
        List<TacticalEmissionSummaryDto> allEmissions = emissionTools.getAllEmissions();
        assertNotNull(allEmissions);
        assertTrue(allEmissions.size() >= 150, "En az 150 radar emisyonu olmalıdır.");

        List<TacticalComintSummaryDto> allComints = comintTools.getAllComints();
        assertNotNull(allComints);
        assertTrue(allComints.size() >= 100, "En az 100 muhabere kaydı olmalıdır.");
    }
}
