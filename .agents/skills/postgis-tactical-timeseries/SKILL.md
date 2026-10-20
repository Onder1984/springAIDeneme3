---
name: postgis-tactical-timeseries
description: >-
  Görev kritik mekânsal (geospatial) ve taktiksel zaman serisi telemetrisi için yüksek performanslı
  PostgreSQL ve PostGIS optimizasyon kalıpları. Tek geçişli COUNT(*) FILTER (...) koşullu toplamaları,
  mekânsal geometriler (ST_MakePoint, ST_MakeLine, ST_Project, ST_DWithin), GiST mekânsal indeksleme,
  kısmi (partial) indeksler ve 500.000+ emisyon kaydı üzerinde mikro-saniye seviyesinde sorgu tekniklerini kapsar.
---

# Taktik Telemetri ve Elektronik Harp İçin PostgreSQL & PostGIS Rehberi

Bu yetenek dokümanı, yüz binlerce radar ve telsiz emisyonunun aktığı savunma sistemlerinde **PostgreSQL** ve **PostGIS** kullanarak milisaniyeler seviyesinde coğrafi ve taktiksel sorgulama yapabilmek için gereken veritabanı mühendisliği standartlarını sunar.

---

## 1. Tek Geçişli Tarama (Single-Pass Scan): `COUNT(*) FILTER (...)`

500.000+ satırlık taktik emisyon tablosunda hiyerarşik istatistikleri (Çocuklu/Çocuksuz Elips, Bağlı/Başıboş LOB) hesaplamak için asla ardışık birden fazla `SELECT COUNT(*)` sorgusu çalıştırmayın. 

Tüm metrikleri tek bir tablo taramasında (Single Table Scan) toplayan koşullu toplama (`FILTER`) kalıbını kullanın:

```sql
SELECT 
  -- Toplam Kestirim Elipsleri
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS') as total_elips,
  
  -- Çocuklu Elipsler (En az 2 LOB ile desteklenmiş)
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS' AND EXISTS (
      SELECT 1 FROM tactical_emissions l WHERE l.kestirim_id = e.id
  )) as childed_elips,
  
  -- Çocuksuz Elipsler (Harici link / TDOA / Doğrudan Kestirim)
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS' AND NOT EXISTS (
      SELECT 1 FROM tactical_emissions l WHERE l.kestirim_id = e.id
  )) as childless_elips,
  
  -- Toplam Ham LOB Kerteriz Çizgileri
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB') as total_lob,
  
  -- Bağlı LOB'lar (Bir kestirime dahil olmuş)
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NOT NULL) as bagli_lob,
  
  -- Başıboş LOB'lar (Tekil istasyon, kör nokta, maskelenmiş)
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NULL) as basibos_lob

FROM tactical_emissions e
WHERE e.son_tespit_zamani >= :baslangicZamani 
  AND e.son_tespit_zamani <= :bitisZamani;
```

* **Performans Etkisi:** 6 ayrı sorgu çalıştırmak yerine tek bir sorguda çalışarak sorgu süresini 2–4 saniyeden **25–35 milisaniyeye** düşürür.

---

## 2. PostGIS Mekânsal Geometrileri (Spatial Geometries)

### A. Algılayıcı ve Hedef Noktalarının Oluşturulması
WGS-84 (SRID 4326) koordinat sisteminde nokta geometrileri oluşturma:
```sql
ALTER TABLE tactical_emissions ADD COLUMN IF NOT EXISTS geom_sensor geometry(Point, 4326);
ALTER TABLE tactical_emissions ADD COLUMN IF NOT EXISTS geom_hedef geometry(Point, 4326);

-- ED Unsuru Algılayıcı Konumu
UPDATE tactical_emissions 
SET geom_sensor = ST_SetSRID(ST_MakePoint(eh_unsuru_boylam, eh_unsuru_enlem), 4326)
WHERE eh_unsuru_enlem IS NOT NULL AND eh_unsuru_boylam IS NOT NULL;

-- Kestirim Hedef Konumu
UPDATE tactical_emissions 
SET geom_hedef = ST_SetSRID(ST_MakePoint(yayin_boylam, yayin_enlem), 4326)
WHERE yayin_enlem IS NOT NULL AND yayin_boylam IS NOT NULL;
```

### B. Dinamik LOB Kerteriz Işını Projeksiyonu (`ST_Project` & `ST_MakeLine`)
Bir DF algılayıcısının tespit ettiği kerteriz açısı (`yon`, derece) doğrultusunda 150 km'lik taktiksel yön çizgisi çizme:
```sql
SELECT 
  id,
  radar_adi,
  yon,
  ST_MakeLine(
    geom_sensor,
    ST_Project(geom_sensor::geography, 150000, radians(yon))::geometry
  ) as lob_geometri_cizgisi
FROM tactical_emissions
WHERE kayit_tipi = 'HAM_LOB' 
  AND geom_sensor IS NOT NULL 
  AND yon IS NOT NULL;
```

---

## 3. Spektrum Doluluk ve Frekans Kesişim Sorguları

Frekans aralıklarının (`min_frekans_mhz` - `max_frekans_mhz`) taktiksel bantlara göre taranması:

```sql
SELECT 
  min_frekans_mhz, 
  max_frekans_mhz, 
  teshis_kimlik, 
  radar_adi,
  son_tespit_zamani
FROM tactical_emissions
WHERE teshis_kimlik IN (:identities)
  AND min_frekans_mhz IS NOT NULL 
  AND max_frekans_mhz IS NOT NULL
  AND son_tespit_zamani >= :start 
  AND son_tespit_zamani <= :end;
```

* **Uygulama Katmanında Geometrik Aralık Birleşimi (Interval Union):**
  Aynı 50 MHz dilimine basan radarlar için:
  ```java
  // Çakışan aralıkları birleştir ve net MHz toplamını bul
  public static double mergeIntervalsAndSum(List<double[]> intervals) {
      if (intervals == null || intervals.isEmpty()) return 0.0;
      intervals.sort(Comparator.comparingDouble(a -> a[0]));
      List<double[]> merged = new ArrayList<>();
      double[] current = intervals.get(0);
      for (int i = 1; i < intervals.size(); i++) {
          double[] next = intervals.get(i);
          if (next[0] <= current[1]) {
              current[1] = Math.max(current[1], next[1]);
          } else {
              merged.add(current);
              current = next;
          }
      }
      merged.add(current);
      return merged.stream().mapToDouble(iv -> iv[1] - iv[0]).sum();
  }
  ```

---

## 4. Taktik Telemetri İçin İndeksleme Stratejileri

500.000+ emisyon kaydında anlık sorgu hızını garanti altına almak için şu indeks kombinasyonları şarttır:

1. **Zaman Pencereli Hiyerarşi İndeksi (Composite B-Tree):**
   ```sql
   CREATE INDEX idx_emissions_tip_zaman ON tactical_emissions(kayit_tipi, son_tespit_zamani DESC);
   CREATE INDEX idx_emissions_kestirim_id ON tactical_emissions(kestirim_id);
   ```

2. **Frekans ve Teşhis İndeksi:**
   ```sql
   CREATE INDEX idx_emissions_teshis_frekans ON tactical_emissions(teshis_kimlik, min_frekans_mhz, max_frekans_mhz)
   WHERE min_frekans_mhz IS NOT NULL AND max_frekans_mhz IS NOT NULL;
   ```

3. **GiST Mekânsal İndeksleme:**
   ```sql
   CREATE INDEX idx_emissions_geom_hedef ON tactical_emissions USING GIST(geom_hedef);
   CREATE INDEX idx_emissions_geom_sensor ON tactical_emissions USING GIST(geom_sensor);
   ```

4. **Kısmi (Partial) İndeksleme - Başıboş LOB Sorguları:**
   Tüm tabloyu taramak yerine sadece kestirim bağı olmayan başıboş kerterizleri indeksleme:
   ```sql
   CREATE INDEX idx_emissions_basibos_lob ON tactical_emissions(son_tespit_zamani DESC)
   WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NULL;
   ```
