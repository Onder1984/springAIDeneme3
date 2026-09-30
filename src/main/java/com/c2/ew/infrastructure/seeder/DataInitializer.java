package com.c2.ew.infrastructure.seeder;

import com.c2.ew.domain.enums.*;
import com.c2.ew.domain.model.AtpEntry;
import com.c2.ew.domain.model.FrequencyEntry;
import com.c2.ew.domain.model.PriEntry;
import com.c2.ew.domain.model.PwEntry;
import com.c2.ew.infrastructure.entity.TacticalEmissionEntity;
import com.c2.ew.infrastructure.repository.TacticalEmissionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TacticalEmissionJpaRepository emissionRepo;
    private final ObjectMapper objectMapper;

    public DataInitializer(TacticalEmissionJpaRepository emissionRepo, ObjectMapper objectMapper) {
        this.emissionRepo = emissionRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        long count = emissionRepo.count();
        if (count > 0) {
            log.info("🛡️ [VERİ KALICILIĞI AKTİF] PostgreSQL'de mevcut {} adet Taktik Emisyon kaydı tespit edildi. Mevcut veriler korunuyor.", count);
            return;
        }

        log.info("🚀 [DATA SEEDER] Taktik Emisyon veritabanı boş. 37 alanlı 160 adet gerçekçi radar emisyonu üretiliyor...");

        List<TacticalEmissionEntity> emissions = new ArrayList<>();
        Random rnd = new Random(42);

        String[] radarModels = {
            "Pantsir-S1 1RS2-1E", "S-400 92N6E Grave Stone", "Patriot AN/MPQ-53",
            "Nebo-M VHF/L", "KORAL-EW Jammer", "AN/APG-77 AESA", "AN/APG-81 AESA",
            "Sea Sparrow Mk-95", "Aegis SPY-1D", "Buk-M2 9S36", "Tor-M2 9A331",
            "S-300PMU2 Flap Lid", "Barış Kartalı MESA", "Aselsan EIRS", "Kalkan Hava Savunma Radarı"
        };

        String[] hssSystems = {
            "S-400 Triumf", "Pantsir-S1 Bataryası", "Patriot PAC-3", "Hisar-O",
            "S-300PMU2", "Iron Dome", "Hawk XXI", "Buk-M2", "Tor-M2", "G-Sınıfı Fırkateyn"
        };

        String[] locations = {
            "Ege Denizi / Sakız Batısı", "Akdeniz / Girit Açıkları", "Suriye / İdlib Kuzeyi",
            "Karadeniz / Kırım Güneyi", "Kıbrıs / Baf Açıkları", "Irak / Kandil Sektörü",
            "Ege Denizi / Midilli Doğusu", "Marmara / Bandırma Hava Sahası", "Güneydoğu / Şırnak Hudut Hattı"
        };

        String[] mevziler = {
            "1420 Rakımlı Tepe", "Hava Meydan Komutanlığı Mevzisi", "Liman Batı İskelesi",
            "Mobil Fırlatma Rampası Alpha", "Radar İstasyon Tepesi", "Fırkateyn Ana Direği", "Hudut Karakolu Yanı"
        };

        String[] ehUnsurleri = {
            "KORAL-1 EH Sistemi", "KORAL-2 EH Sistemi", "İHA-DF-Pod-1", "İHA-DF-Pod-2",
            "TCG-GÖKSU F-497", "RADNET-KARA-04", "HAVA-SOJ-01", "DF-KARA-BATARYA-03"
        };

        String[] tailNumbers = {
            "TUAF-201", "TUAF-104", "RF-95412", "HAF-512", "USAF-88-041", "TUAF-93-012", "RF-81720"
        };

        Instant now = Instant.now();

        for (int i = 1; i <= 160; i++) {
            TacticalEmissionEntity e = new TacticalEmissionEntity();
            e.setId(String.format("EMI-%04d", 1000 + i));

            // Veri Kaynağı
            VeriKaynagi vk = VeriKaynagi.values()[rnd.nextInt(VeriKaynagi.values().length)];
            e.setVeriKaynagi(vk);
            e.setKuvvetSiraNo(String.format("%s-%03d-2026", vk.name(), 100 + i));
            e.setUstBirlikRef(vk.name() + " Komutanlığı MEBS ve EH Daire Bşk.");
            e.setEhUnsuru(ehUnsurleri[rnd.nextInt(ehUnsurleri.length)]);

            // EH Unsuru Konumu ve Yönü
            double ehLat = 38.0 + (rnd.nextDouble() * 3.5);
            double ehLon = 27.0 + (rnd.nextDouble() * 8.0);
            e.setEhUnsuruEnlem(ehLat);
            e.setEhUnsuruBoylam(ehLon);
            e.setEhUnsuruIrtifa(100.0 + rnd.nextInt(900));
            e.setYon(rnd.nextDouble() * 360.0);

            // Yayın Konumu (Elips)
            double targetLat = ehLat + (rnd.nextDouble() * 0.8 - 0.4);
            double targetLon = ehLon + (rnd.nextDouble() * 0.8 - 0.4);
            e.setYayinEnlem(targetLat);
            e.setYayinBoylam(targetLon);
            e.setYayinSemiMajorMeters(300.0 + rnd.nextInt(1500));
            e.setYayinSemiMinorMeters(150.0 + rnd.nextInt(600));
            e.setYayinOrientationDegrees(rnd.nextDouble() * 180.0);

            e.setHedefYerBilgisi(locations[rnd.nextInt(locations.length)]);
            e.setHedefMevziBilgisi(mevziler[rnd.nextInt(mevziler.length)]);

            // Teşhis Kimlik (Ağırlıklı DÜŞMAN / DOST / BİLİNMEYEN)
            int teshisRoll = rnd.nextInt(100);
            TeshisKimlik tk;
            if (teshisRoll < 35) tk = TeshisKimlik.DUSMAN;
            else if (teshisRoll < 65) tk = TeshisKimlik.DOST;
            else if (teshisRoll < 80) tk = TeshisKimlik.MUHTEMEL_DUSMAN;
            else if (teshisRoll < 95) tk = TeshisKimlik.BILINMEYEN;
            else tk = TeshisKimlik.TARAFSIZ;
            e.setTeshisKimlik(tk);

            e.setElintNotasyonu(String.format("ELNOT-%c%03d", (char)('A' + rnd.nextInt(6)), rnd.nextInt(900) + 100));
            e.setHedefKaynagi(HedefKaynagi.values()[rnd.nextInt(HedefKaynagi.values().length)]);
            e.setPolarizasyon(Polarizasyon.values()[rnd.nextInt(Polarizasyon.values().length)]);
            e.setSpotNo(String.format("SP%04d", rnd.nextInt(9000) + 1000));
            e.setPulseCw(rnd.nextInt(10) < 8 ? PulseCw.PULSE : PulseCw.CW);

            String model = radarModels[rnd.nextInt(radarModels.length)];
            e.setRadarAdi(model);
            if (model.contains("Pantsir") || model.contains("92N6E") || model.contains("MPQ-53") || model.contains("Mk-95")) {
                e.setRadarGorevi("Atış Kontrol ve Füze Güdüm");
            } else if (model.contains("Nebo-M") || model.contains("Barış Kartalı") || model.contains("EIRS")) {
                e.setRadarGorevi("Erken İhbar ve Hava Resmi");
            } else {
                e.setRadarGorevi("Hedef Takip ve Arama");
            }

            // Platform
            if (model.contains("AN/APG") || model.contains("Barış Kartalı")) {
                e.setPlatformOrtami(PlatformOrtami.HAVA);
                e.setPlatformTipi(PlatformTipi.HAVA);
                e.setUcakKuyrukNumarasi(tailNumbers[rnd.nextInt(tailNumbers.length)]);
            } else if (model.contains("Sea Sparrow") || model.contains("SPY-1D")) {
                e.setPlatformOrtami(PlatformOrtami.SU_USTU);
                e.setPlatformTipi(PlatformTipi.SU_USTU);
            } else {
                e.setPlatformOrtami(PlatformOrtami.KARA);
                e.setPlatformTipi(PlatformTipi.KARA);
            }

            e.setGenlikDbm(-85.0 + rnd.nextDouble() * 55.0);
            e.setModulasyon(Modulasyon.values()[rnd.nextInt(Modulasyon.values().length)]);

            // Taciz & ET Uygulama Durumu
            boolean isEnemy = (tk == TeshisKimlik.DUSMAN || tk == TeshisKimlik.MUHTEMEL_DUSMAN);
            boolean taciz = isEnemy && (rnd.nextInt(10) < 5);
            e.setTaciz(taciz);

            if (isEnemy && taciz) {
                int etRoll = rnd.nextInt(10);
                if (etRoll < 5) e.setEtUygulamaDurumu(EtUygulamaDurumu.UYGULANIYOR);
                else if (etRoll < 8) e.setEtUygulamaDurumu(EtUygulamaDurumu.SUSTURDU);
                else e.setEtUygulamaDurumu(EtUygulamaDurumu.UYGULANMIYOR);
            } else {
                e.setEtUygulamaDurumu(rnd.nextBoolean() ? EtUygulamaDurumu.UYGULANMIYOR : EtUygulamaDurumu.TANIMSIZ);
            }

            // Zamanlar (Son 24 saat içinde)
            int minutesAgo = rnd.nextInt(1440);
            Instant lastSeen = now.minus(minutesAgo, ChronoUnit.MINUTES);
            int durationSec = rnd.nextInt(3600) + 60;
            Instant firstSeen = lastSeen.minus(durationSec, ChronoUnit.SECONDS);
            e.setIlkTespitZamani(firstSeen);
            e.setSonTespitZamani(lastSeen);
            e.setSureSn((double) durationSec);

            e.setHss(hssSystems[rnd.nextInt(hssSystems.length)]);
            e.setRadarKesitAlani(0.5 + rnd.nextDouble() * 15.0);
            e.setRadarIzNumarasi(String.format("TRK-%05d", rnd.nextInt(90000) + 10000));

            // Çoklu Frekans Listesi (Aralıklı)
            double baseFreq;
            if (model.contains("Nebo-M")) baseFreq = 150.0 + rnd.nextInt(200); // VHF
            else if (model.contains("Pantsir") || model.contains("92N6E") || model.contains("MPQ-53") || model.contains("AN/APG")) baseFreq = 8500.0 + rnd.nextInt(2500); // X-Band
            else if (model.contains("SPY-1D")) baseFreq = 3100.0 + rnd.nextInt(400); // S-Band
            else baseFreq = 5000.0 + rnd.nextInt(4000);

            List<FrequencyEntry> fList = new ArrayList<>();
            fList.add(new FrequencyEntry(
                rnd.nextBoolean() ? "ATLAMALI_HOPPING" : "SABIT",
                baseFreq,
                baseFreq + (rnd.nextInt(150) + 20)
            ));
            if (rnd.nextBoolean()) {
                fList.add(new FrequencyEntry(
                    "DEGISKEN_JITTER",
                    baseFreq + 200,
                    baseFreq + 400
                ));
            }
            e.setMinFrekansMhz(fList.stream().mapToDouble(FrequencyEntry::minFrekansMhz).min().orElse(baseFreq));
            e.setMaxFrekansMhz(fList.stream().mapToDouble(FrequencyEntry::maxFrekansMhz).max().orElse(baseFreq + 100));
            e.setFrekansListesiJson(objectMapper.writeValueAsString(fList));

            // Çoklu PRI Listesi (µs)
            double basePri = 400.0 + rnd.nextInt(1600);
            List<PriEntry> priList = new ArrayList<>();
            priList.add(new PriEntry(rnd.nextBoolean() ? "KADEMELI" : "SABIT", basePri, basePri + rnd.nextInt(200)));
            if (rnd.nextBoolean()) {
                priList.add(new PriEntry("DWELL_SWITCH", basePri + 300, basePri + 600));
            }
            e.setMinPriMicroSec(priList.stream().mapToDouble(PriEntry::minPriMicroSec).min().orElse(basePri));
            e.setMaxPriMicroSec(priList.stream().mapToDouble(PriEntry::maxPriMicroSec).max().orElse(basePri + 200));
            e.setPriListesiJson(objectMapper.writeValueAsString(priList));

            // Çoklu PW Listesi (µs)
            double basePw = 0.5 + rnd.nextDouble() * 5.0;
            List<PwEntry> pwList = new ArrayList<>();
            pwList.add(new PwEntry(rnd.nextBoolean() ? "MODULASYONLU" : "SABIT", basePw, basePw + rnd.nextDouble() * 2.0));
            e.setMinPwMicroSec(pwList.stream().mapToDouble(PwEntry::minPwMicroSec).min().orElse(basePw));
            e.setMaxPwMicroSec(pwList.stream().mapToDouble(PwEntry::maxPwMicroSec).max().orElse(basePw + 1.0));
            e.setPwListesiJson(objectMapper.writeValueAsString(pwList));

            // Çoklu ATP Listesi (Anten Tarama Periyodu - µs)
            double baseAtp = (1.5 + rnd.nextDouble() * 6.0) * 1_000_000.0; // saniyeden µs'ye
            List<AtpEntry> atpList = new ArrayList<>();
            atpList.add(new AtpEntry(rnd.nextBoolean() ? "ELEKTRONIK_TARAMA" : "DAIRESEL", baseAtp));
            e.setMinAtpMicroSec(baseAtp);
            e.setMaxAtpMicroSec(baseAtp + 500_000.0);
            e.setAtpListesiJson(objectMapper.writeValueAsString(atpList));

            // Notlar ve Onay
            if (taciz) {
                e.setOperatorNotu("Düşman unsuru hudut hattını taciz etti. Elektronik taarruz tedbirleri devreye alındı.");
                e.setOnaylayanKullanici("Bnb. Serdar Yılmaz");
                e.setOnayNotu("Taktik EH Müdahalesi Onaylandı.");
            } else {
                e.setOperatorNotu("Rutin taktik radar izleme ve kestirim kaydı.");
                e.setOnaylayanKullanici("Yzb. Can Demir");
                e.setOnayNotu("Kayıt incelendi, kütüphane imzası ile eşleşti.");
            }

            emissions.add(e);
        }

        emissionRepo.saveAll(emissions);
        log.info("✅ [DATA SEEDER] 160 adet Taktik Radar Emisyonu başarıyla PostgreSQL'e (tactical_ew_db_3) kaydedildi.");
    }
}
