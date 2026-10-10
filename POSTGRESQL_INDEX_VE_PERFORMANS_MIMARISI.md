# 🛡️ PostgreSQL 18 & PostGIS 3.6 İndeks ve Performans Mimarisi

Bu doküman, Elektronik Harp (Radar/ELINT ve Muhabere/COMINT) Komuta Kontrol (C2) sistemimizin veritabanı indeks mimarisini, JPA entity tanımlarını ve sorgu optimizasyon stratejilerini ayrıntılı olarak açıklamaktadır.

---

## 1. Mimaride İndekslerin Rolü ve Önemi

Sistemimiz gerçek zamanlı taktik sahada binlerce radar emisyonunu, kerteriz hatlarını (LOB) ve telsiz yayınlarını eşzamanlı olarak işlemektedir (`tactical.streamer` her 5 saniyede bir yeni kayıtlar üretir). 
Copilot ajanı (Spring AI) doğal dil sorularını yanıtlarken doğrudan PostgreSQL üzerinde filtreleme, aralık taraması (range scan) ve mekânsal (geospatial) füzyon sorguları çalıştırır.

Doğru indeksleme sayesinde:
1. **Tam Tablo Taraması (Seq Scan) Engellenir:** 500.000+ emisyon kaydında bile sorgular milisaniyeler (1-5 ms) mertebesinde tamamlanır.
2. **LLM Yanıt Süresi Düşer:** Yapay zeka ajanının araç çağrıları (Tool Calling) veriyi diskten anında çektiği için brifingler 3-4 saniyede kullanıcıya ulaşır.
3. **Mekânsal Füzyon Hızlanır:** PostGIS GiST indeksleri ile 10-50 km çapındaki CEP hata elipslerinin kesişimi donanım seviyesinde geometrik indekslerle hesaplanır.

---

## 2. Projede Bu Tanımlar Nerede Yer Alıyor?

Projemizde indeks tanımları iki katmanda yönetilmektedir:
1. **JPA / Hibernate Entity Katmanı (Java Kaynak Kodları)**
2. **PostgreSQL / PostGIS Uzantı ve Fonksiyonel İndeks Katmanı (Veritabanı Motoru)**

### A. Radar (ELINT) İndeks Tanımları
* **Dosya Konumu:** `src/main/java/com/c2/ew/infrastructure/entity/TacticalEmissionEntity.java` (Satır 8 - 22)
* **Kaynak Kod Bloğu:**
```java
@Entity
@Table(name = "tactical_emissions", indexes = {
    @Index(name = "idx_emi_teshis", columnList = "teshis_kimlik"),
    @Index(name = "idx_emi_veri_kaynagi", columnList = "veri_kaynagi"),
    @Index(name = "idx_emi_radar_adi", columnList = "radar_adi"),
    @Index(name = "idx_emi_platform_ortami", columnList = "platform_ortami"),
    @Index(name = "idx_emi_taciz", columnList = "taciz"),
    @Index(name = "idx_emi_et_durumu", columnList = "et_uygulama_durumu"),
    @Index(name = "idx_emi_freq_range", columnList = "min_frekans_mhz, max_frekans_mhz"),
    @Index(name = "idx_emi_pri_range", columnList = "min_pri_micro_sec, max_pri_micro_sec"),
    @Index(name = "idx_emi_pw_range", columnList = "min_pw_micro_sec, max_pw_micro_sec"),
    @Index(name = "idx_emi_atp_range", columnList = "min_atp_micro_sec, max_atp_micro_sec"),
    @Index(name = "idx_emi_son_tespit", columnList = "son_tespit_zamani"),
    @Index(name = "idx_emi_kayit_tipi", columnList = "kayit_tipi"),
    @Index(name = "idx_emi_kestirim_id", columnList = "kestirim_id")
})
public class TacticalEmissionEntity {
    // ...
}
```

### B. Muhabere (COMINT) İndeks Tanımları
* **Dosya Konumu:** `src/main/java/com/c2/ew/infrastructure/entity/TacticalComintEntity.java` (Satır 8 - 22)
* **Kaynak Kod Bloğu:**
```java
@Entity
@Table(name = "tactical_comint", indexes = {
    @Index(name = "idx_com_teshis", columnList = "teshis_kimlik"),
    @Index(name = "idx_com_veri_kaynagi", columnList = "veri_kaynagi"),
    @Index(name = "idx_com_lisan", columnList = "lisan"),
    @Index(name = "idx_com_protokol", columnList = "protokol"),
    @Index(name = "idx_com_modulasyon", columnList = "modulasyon"),
    @Index(name = "idx_com_tip", columnList = "tip"),
    @Index(name = "idx_com_haberlesme_sekli", columnList = "haberlesme_sekli"),
    @Index(name = "idx_com_cagri_adi", columnList = "cagri_adi"),
    @Index(name = "idx_com_karsi_cagri_adi", columnList = "karsi_cagri_adi"),
    @Index(name = "idx_com_freq_range", columnList = "min_frekans_mhz, max_frekans_mhz"),
    @Index(name = "idx_com_son_tespit", columnList = "son_tespit_zamani"),
    @Index(name = "idx_com_kayit_tipi", columnList = "kayit_tipi"),
    @Index(name = "idx_com_kestirim_id", columnList = "kestirim_id")
})
public class TacticalComintEntity {
    // ...
}
```

### C. Veritabanı Seviyesindeki PostGIS GiST Mekânsal İndeksleri
Harita üzerinde nokta/mesafe hesaplamalarında (özellikle `ST_DWithin` ve coğrafi örtüşme analizlerinde) enlem ve boylam çifti doğrudan PostGIS GiST indeksleriyle hızlandırılmıştır:
```sql
-- Radar Mekânsal İndeksi
CREATE INDEX idx_emi_spatial_gist ON public.tactical_emissions 
USING GIST (ST_SetSRID(ST_MakePoint(yayin_boylam, yayin_enlem), 4326));

-- Muhabere Mekânsal İndeksi
CREATE INDEX idx_com_spatial_gist ON public.tactical_comint 
USING GIST (ST_SetSRID(ST_MakePoint(yayin_boylam, yayin_enlem), 4326));
```

---

## 3. İndeks Analiz ve Kullanım Matrisi

### A. Radar (ELINT) İndeksleri Tablosu
| # | İndeks Adı | Kolonlar | Tip | İlgili Tool / Sorgu | Operasyonel Açıklama |
|---|---|---|---|---|---|
| 1 | `tactical_emissions_pkey` | `emission_id` | B-Tree (Unique) | `getEmissionTechnicalDetails` | Emisyon ID bazlı tekil detay çekme |
| 2 | `idx_emi_teshis` | `teshis_kimlik` | B-Tree | `queryPriorityThreats`, `getTacticalMacroStats` | Düşman/Dost/Bilinmeyen filtreleri |
| 3 | `idx_emi_kayit_tipi` | `kayit_tipi` | B-Tree | `analyzeRadarHierarchy` | Kestirim Elipsi ve Ham LOB ayrımı |
| 4 | `idx_emi_kestirim_id` | `kestirim_id` | B-Tree | `analyzeRadarHierarchy` | Bağlı LOB vs Başıboş LOB tespiti (`IS NULL` / `IS NOT NULL`) |
| 5 | `idx_emi_veri_kaynagi` | `veri_kaynagi` | B-Tree | `getTacticalMacroStats`, `queryEmissions` | Kara, Hava, Deniz sensör dağılımı |
| 6 | `idx_emi_radar_adi` | `radar_adi` | B-Tree | `queryEmissions` | Radar adı (AN/MPQ-64, Bar Lock vb.) aramaları |
| 7 | `idx_emi_platform_ortami` | `platform_ortami` | B-Tree | `getElectronicOrderOfBattleSummary` | Hava/Kara/Deniz EOB hiyerarşisi |
| 8 | `idx_emi_taciz` | `taciz` | B-Tree | `getDetailedJammingAndHarassmentAnalysis` | Sınır tacizi yapan düşman unsurları |
| 9 | `idx_emi_et_durumu` | `et_uygulama_durumu` | B-Tree | `getElectronicAttackReport` | Susturulan veya ET uygulanan radarlar |
| 10 | `idx_emi_freq_range` | `min_frekans_mhz, max_frekans_mhz` | B-Tree (Bileşik) | `queryEmissions` | X-Band, S-Band frekans aralığı taraması |
| 11 | `idx_emi_pri_range` | `min_pri_micro_sec, max_pri_micro_sec` | B-Tree (Bileşik) | `analyzeRadarWaveformAgility`, `queryEmissions` | Darbe tekrarlama (PRI) filtreleri |
| 12 | `idx_emi_pw_range` | `min_pw_micro_sec, max_pw_micro_sec` | B-Tree (Bileşik) | `queryEmissions` | Darbe genişliği (PW) filtreleri |
| 13 | `idx_emi_atp_range` | `min_atp_micro_sec, max_atp_micro_sec` | B-Tree (Bileşik) | `queryEmissions` | Anten tarama deseni (ATP) periyotları |
| 14 | `idx_emi_son_tespit` | `son_tespit_zamani` | B-Tree | `analyzeRadarHierarchy`, Zaman filtreleri | "Son 1 saat", "Son 24 saat", "Bugün" sorguları |
| 15 | `idx_emi_spatial_gist` | `ST_MakePoint(...)` | GiST (Spatial) | `fuseRadarAndComintNodes` | Coğrafi koordinat ve CEP kesişim füzyonu |

### B. Muhabere (COMINT) İndeksleri Tablosu
| # | İndeks Adı | Kolonlar | Tip | İlgili Tool / Sorgu | Operasyonel Açıklama |
|---|---|---|---|---|---|
| 1 | `tactical_comint_pkey` | `comint_id` | B-Tree (Unique) | `getComintEmissionDetails` | Telsiz yayını tekil detay çekme |
| 2 | `idx_com_cagri_adi` | `cagri_adi` | B-Tree | `getMostActiveCallsigns`, `queryComintCallsignNetwork` | En çok konuşan telsiz çağrı adları |
| 3 | `idx_com_karsi_cagri_adi` | `karsi_cagri_adi` | B-Tree | `getCallsignPairInteractionStats` | Link-pair (Kim kiminle konuştu) analizi |
| 4 | `idx_com_haberlesme_sekli`| `haberlesme_sekli` | B-Tree | `getComsecAndTransmissionTactics` | Kripto, Ses, Veri, Karıştırma Gayda |
| 5 | `idx_com_lisan` | `lisan` | B-Tree | `queryComintEmissions` | Dil bazlı (Rusça, Arapça vb.) filtreleme |
| 6 | `idx_com_protokol` | `protokol` | B-Tree | `queryComintEmissions` | DMR, D-Star, Askeri VHF protokolleri |
| 7 | `idx_com_modulasyon` | `modulasyon` | B-Tree | `queryComintEmissions` | FM, AM, FSK modülasyon tipleri |
| 8 | `idx_com_tip` | `tip` | B-Tree | `getComintMacroStats` | HF vs VHF frekans bandı ayrımı |
| 9 | `idx_com_freq_range` | `min_frekans_mhz, max_frekans_mhz` | B-Tree (Bileşik) | `queryComintEmissions` | Telsiz frekans bandı aralığı sorgusu |
| 10 | `idx_com_teshis` | `teshis_kimlik` | B-Tree | `queryPriorityComintThreats` | Düşman telsiz ağlarının anında listelenmesi |
| 11 | `idx_com_veri_kaynagi` | `veri_kaynagi` | B-Tree | `getComintMacroStats` | Kara, Hava, Deniz sensör kaynağı |
| 12 | `idx_com_kayit_tipi` | `kayit_tipi` | B-Tree | `analyzeComintHierarchy` | Kestirim Elipsi ve Telsiz LOB ayrımı |
| 13 | `idx_com_kestirim_id` | `kestirim_id` | B-Tree | `analyzeComintHierarchy` | Başıboş vs Bağlı kerteriz hatları |
| 14 | `idx_com_son_tespit` | `son_tespit_zamani` | B-Tree | `getComintTrafficTrend` | Telsiz trafik trendi ve saatlik hareketlenmeler (Surge) |
| 15 | `idx_com_spatial_gist` | `ST_MakePoint(...)` | GiST (Spatial) | `fuseRadarAndComintNodes` | Radar + Telsiz ortak mevzi korelasyonu |

---

## 4. Özel İndeksleme Stratejileri

### 1. Bileşik Aralık (Range) İndeksleri
Radarlar ve telsizler tek bir frekansta değil, çalışma bant aralıklarında (örn. 8500 MHz - 9600 MHz) yayın yapar.
`(min_frekans_mhz, max_frekans_mhz)` bileşik indeksi sayesinde veritabanı iki ayrı kolonu taramak yerine B-Tree üzerinde tek bir aralık araması yapar:
```sql
-- İndeks doğrudan devreye girer:
SELECT * FROM tactical_emissions 
WHERE min_frekans_mhz >= 8000 AND max_frekans_mhz <= 12000;
```

### 2. Hiyerarşi (LOB & Kestirim) İndeksleri
Doktrin kurallarımıza göre kestirimler ebeveyn, LOB'lar ise çocuk nesnelerdir:
* `kestirim_id IS NOT NULL`: Kestirime dahil olmuş bağlı LOB'lar.
* `kestirim_id IS NULL`: Hiçbir kestirime dahil olmamış başıboş (stray) LOB'lar.
`idx_emi_kestirim_id` indeksi sayesinde yüz binlerce LOB kaydı arasından başıboş LOB'lar anında süzülür.

### 3. PostGIS GiST Mekânsal İndeksleri
İki emisyon arasındaki kuş uçuşu mesafeyi ya da CEP elips kesişimini bulurken:
```sql
-- GiST indeksi koordinatları R-Tree benzeri kutulara böler:
SELECT * FROM tactical_emissions e
JOIN tactical_comint c 
  ON ST_DWithin(
       ST_SetSRID(ST_MakePoint(e.yayin_boylam, e.yayin_enlem), 4326)::geography,
       ST_SetSRID(ST_MakePoint(c.yayin_boylam, c.yayin_enlem), 4326)::geography,
       10000 -- 10 km
     );
```
Bu sorgu 1000'lerce emisyon arasında $O(N^2)$ yerine $O(N \log N)$ karmaşıklıkla çözülür.

---

## 5. PostgreSQL İndeks Doğrulama Sorguları

İndekslerin aktifliğini ve boyutlarını terminalden incelemek için:

```sql
-- Tüm public indeksleri listele
SELECT tablename, indexname, indexdef 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;

-- İndeks boyutlarını ve kullanım istatistiklerini kontrol et
SELECT
    relname AS tablo_veya_indeks,
    pg_size_pretty(pg_relation_size(relid)) AS boyut
FROM pg_statio_user_indexes
ORDER BY pg_relation_size(relid) DESC;
```
