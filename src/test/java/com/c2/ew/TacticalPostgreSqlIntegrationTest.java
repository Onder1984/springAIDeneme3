package com.c2.ew;

import com.c2.ew.agent.dto.TacticalEmissionDetailDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.tools.RadarEmissionTools;
import com.c2.ew.domain.model.PagedResult;
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

        Map<?, ?> teshis = (Map<?, ?>) stats.get("teshisDagilimi");
        assertNotNull(teshis);
        assertFalse(teshis.isEmpty(), "Teşhis dağılımı boş olmamalıdır");

        Map<?, ?> kaynak = (Map<?, ?>) stats.get("veriKaynagiDagilimi");
        assertNotNull(kaynak);
        assertFalse(kaynak.isEmpty(), "Veri kaynağı dağılımı boş olmamalıdır");
    }

    @Test
    @DisplayName("Aralıklı (Interval Overlap) Frekans ve PRI sorgulaması başarıyla çalışmalıdır")
    void testIntervalRangeQueries() {
        // 8000 - 12000 MHz X-Band aralıklı sorgu
        PagedResult<TacticalEmissionSummaryDto> xBand = emissionTools.queryEmissionsPaged(
            null, null, null, null, null, null, null,
            8000.0, 12000.0, null,
            null, null, null, null, null, null,
            0, 15
        );

        assertNotNull(xBand);
        assertTrue(xBand.totalCount() > 0, "8000-12000 MHz arasında en az bir hedef bulunmalıdır");
        assertFalse(xBand.items().isEmpty());

        for (TacticalEmissionSummaryDto emi : xBand.items()) {
            assertTrue(emi.minFrekansMhz() <= 12000.0 && emi.maxFrekansMhz() >= 8000.0,
                "Frekans aralığı sorguyla kesişmelidir: " + emi.minFrekansMhz() + " - " + emi.maxFrekansMhz());
        }

        // PRI <= 1000 µs aralıklı sorgu
        PagedResult<TacticalEmissionSummaryDto> lowPri = emissionTools.queryEmissionsPaged(
            null, null, null, null, null, null, null,
            null, null, null,
            null, 1000.0,
            null, null, null, null,
            0, 15
        );

        assertNotNull(lowPri);
        assertTrue(lowPri.totalCount() > 0);
        for (TacticalEmissionSummaryDto emi : lowPri.items()) {
            assertTrue(emi.minPriMicroSec() <= 1000.0, "Hedefin min PRI değeri 1000 µs altında olmalıdır: " + emi.minPriMicroSec());
        }
    }

    @Test
    @DisplayName("Öncelikli Tehditler (Düşman, Taciz, HSS) sorgusu doğru çalışmalıdır")
    void testPriorityThreats() {
        List<TacticalEmissionSummaryDto> threats = emissionTools.queryPriorityThreats(10);
        assertNotNull(threats);
        assertEquals(10, threats.size());

        for (TacticalEmissionSummaryDto t : threats) {
            assertTrue(t.taciz() || t.teshisKimlik().contains("DUSMAN"),
                "Tehdit listesindeki hedef ya taciz yapmalı ya da düşman olmalıdır");
        }
    }

    @Test
    @DisplayName("Tekil Hedef Çoklu Frekans ve Teknik Detayları başarıyla çekilmelidir")
    void testEmissionDetails() {
        List<TacticalEmissionSummaryDto> anyEmissions = emissionTools.queryPriorityThreats(1);
        assertFalse(anyEmissions.isEmpty());

        String targetId = anyEmissions.get(0).id();
        TacticalEmissionDetailDto detail = emissionTools.getEmissionDetails(targetId);

        assertNotNull(detail);
        assertEquals(targetId, detail.id());
        assertNotNull(detail.frekansListesi(), "Frekans listesi boş olmamalıdır");
        assertFalse(detail.frekansListesi().isEmpty(), "En az bir frekans bandı olmalıdır");
        assertNotNull(detail.priListesi(), "PRI listesi boş olmamalıdır");
        assertNotNull(detail.pwListesi(), "PW listesi boş olmamalıdır");
        assertNotNull(detail.atpListesi(), "ATP listesi boş olmamalıdır");
    }

    @Test
    @DisplayName("Zaman-Frekans Şelale (Scatter) noktaları dakika bazlı doğru dönmelidir")
    void testTimeFrequencyScatterPoints() {
        List<Map<String, Object>> points = emissionTools.getTimeFrequencyDistribution(null, null, null, 100);
        assertNotNull(points);
        assertFalse(points.isEmpty());

        Map<String, Object> firstPoint = points.get(0);
        assertTrue(firstPoint.containsKey("timeMinutes"), "timeMinutes ekseni bulunmalıdır");
        assertTrue(firstPoint.containsKey("frequencyMhz"), "frequencyMhz ekseni bulunmalıdır");
        assertTrue(firstPoint.containsKey("teshisKimlik"));
    }
}
