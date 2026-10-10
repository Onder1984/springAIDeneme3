---
name: postgis-tactical-timeseries
description: >-
  High-performance PostgreSQL and PostGIS optimization patterns for mission-critical
  geospatial and tactical time-series telemetry. Covers single-pass COUNT(*) FILTER (...)
  aggregation, spatial geometries (ST_MakePoint, ST_MakeLine, ST_DWithin), GiST/SP-GiST
  spatial indexing, and time-partitioning for 500K+ emission records.
---

# PostgreSQL & PostGIS for Tactical Telemetry

This skill guides high-throughput, low-latency database engineering for defense systems handling hundreds of thousands of radar/radio detections.

---

## 1. Single-Pass Scan Aggregation: `COUNT(*) FILTER (...)`

Never execute multiple sequential count queries over 400K+ rows. Use single-pass conditional aggregation:

```sql
SELECT 
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS') as total_elips,
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS' AND EXISTS (
      SELECT 1 FROM tactical_emissions l WHERE l.kestirim_id = e.emission_id
  )) as childed_elips,
  COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS' AND NOT EXISTS (
      SELECT 1 FROM tactical_emissions l WHERE l.kestirim_id = e.emission_id
  )) as childless_elips,
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB') as total_lob,
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NOT NULL) as bagli_lob,
  COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NULL) as basibos_lob
FROM tactical_emissions e
WHERE e.son_tespit_zamani >= :baslangic AND e.son_tespit_zamani <= :bitis;
```
* **Performance:** Executes in 25–35 ms instead of seconds.

---

## 2. Spatial Geometries with PostGIS

### A. Point Representation
```sql
ALTER TABLE tactical_emissions ADD COLUMN geom_sensor geometry(Point, 4326);
ALTER TABLE tactical_emissions ADD COLUMN geom_hedef geometry(Point, 4326);

UPDATE tactical_emissions 
SET geom_sensor = ST_SetSRID(ST_MakePoint(eh_unsuru_boylam, eh_unsuru_enlem), 4326)
WHERE eh_unsuru_enlem IS NOT NULL;
```

### B. Dynamic LOB Vector Ray Projection
Project a bearing ray from the sensor position along its azimuth direction for 150 km:
```sql
SELECT 
  emission_id,
  ST_MakeLine(
    geom_sensor,
    ST_Project(geom_sensor::geography, 150000, radians(yon))::geometry
  ) as lob_line
FROM tactical_emissions
WHERE kayit_tipi = 'HAM_LOB';
```

---

## 3. Indexing Strategies for Defense Telemetry

1. **Composite B-Tree for Time-Bounded Filtering:**
   ```sql
   CREATE INDEX idx_radar_tip_zamani ON tactical_emissions(kayit_tipi, son_tespit_zamani DESC);
   CREATE INDEX idx_radar_kestirim ON tactical_emissions(kestirim_id);
   ```
2. **GiST Spatial Indexing:**
   ```sql
   CREATE INDEX idx_radar_geom_hedef ON tactical_emissions USING GIST(geom_hedef);
   ```
3. **Partial Indexing for Başıboş LOBs:**
   ```sql
   CREATE INDEX idx_radar_basibos ON tactical_emissions(son_tespit_zamani DESC)
   WHERE kayit_tipi = 'HAM_LOB' AND kestirim_id IS NULL;
   ```
