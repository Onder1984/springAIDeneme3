# ⚔️ Taktik Elektronik Harp Copilot: Doğal Dil Sorguları ve Araç (Tool) Doktrin Rehberi

**Doküman Versiyonu:** 2.1.0 (Eksiksiz & Zenginleştirilmiş Tam Sürüm)  
**Sistem:** Taktik Elektronik Harp (EH / C2) Karar Destek ve Analiz Copilotu  
**Hedef Kitle:** Elektronik Harp Subayları (EWO), C2 Operatörleri ve Sistem Geliştiricileri  

---

## 1. GİRİŞ VE ÇALIŞMA PRENSİBİ

Bu rehber, **Spring AI (Gemini 3.5 Flash-Lite)** destekli Taktik C2 Copilot sistemimize yöneltilebilecek tüm doğal dil soru kalıplarını, bu sorular sorulduğunda arka planda tetiklenen **Spring AI Araçlarını (`@Tool`)**, araçların **parametre yapılarını**, **JPA/SQL filtreleme mantıklarını** ve karar mekanizmasının arkasındaki **askeri doktrin gerekçelerini** eksiksiz olarak açıklar.

### Doğal Dil -> Tool Calling -> Deterministik Çıkarım Döngüsü:
```text
┌────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│ OPERATÖR DOĞAL DİL     │ ───> │ GEMINI 3.5 FLASH-LITE   │ ───> │ POSTGRESQL 18 + POSTGIS │
│ "Kritik düşman         │      │ Parametreleri doldurur, │      │ İndeksli JPA Sorgusu,   │
│ radarlarını listele"   │      │ ilgili Tool'u çağırır.  │      │ 1-5 ms içinde çalışır.  │
└────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
                                             │                                │
                                             ▼                                ▼
                                ┌─────────────────────────┐      ┌─────────────────────────┐
                                │ ASKERİ DOKTRİNEL RAPOR  │ <─── │ HAM VERİ ÇIKTISI        │
                                │ Sayısal Tablo, Tehdit   │      │ Record DTO, İstatistik, │
                                │ Analizi & Karşı Tedbir  │      │ LOB/Elips Hiyerarşisi   │
                                └─────────────────────────┘      └─────────────────────────┘
```

---

## 2. DETAYLI KATEGORİ VE ARAÇ (TOOL) REHBERİ

---

### KATEGORİ 1: Makro Durumsal Farkındalık ve Dağılım İstatistikleri

#### 1.1. Radar (ELINT) Makro Durum Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Sahadaki genel radar durumunu ve makro istatistikleri özetle."*
  * *"Kaç radar emisyonu var, kaçı dost kaçı düşman?"*
  * *"Platform ortamlarına göre (hava, deniz, kara, su altı, uzay) radar dağılımı nedir?"*
  * *"Sensör veri kaynaklarına göre (Kara, Deniz, Hava, Genelkurmay) radar dağılımını ver."*
  * *"Sahada toplam kaç kestirim elipsi ve kaç kerteriz hattı (LOB) takip ediliyor?"*
* 🛠️ **Çağrılan Tool:** `getTacticalMacroStats()`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L99-L148`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L99-L148)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  ```sql
  SELECT teshis_kimlik, COUNT(*) FROM tactical_emissions GROUP BY teshis_kimlik;
  SELECT platform_ortami, COUNT(*) FROM tactical_emissions GROUP BY platform_ortami;
  SELECT veri_kaynagi, COUNT(*) FROM tactical_emissions GROUP BY veri_kaynagi;
  SELECT kayit_tipi, COUNT(*) FROM tactical_emissions GROUP BY kayit_tipi;
  ```
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  C2 harekât merkezinde video-wall ekranına yansıtılan üst düzey durumsal farkındalık tablosudur. Harekât sahasındaki kuvvet çarpanını, dost/hasım kuvvet oranını ve sensör kapsama dengesini gösterir.

---

#### 1.2. Muhabere (COMINT) Makro Durum Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Kaç adet dost, düşman ve bilinmeyen telsiz yayını tespit edildi?"*
  * *"Genel telsiz haberleşme trafiğinde kripto ve karıştırma oranları nedir?"*
  * *"Sahada hangi dillerde (Rusça, Arapça, Yunanca vb.) konuşuluyor ve HF/VHF dağılımı nasıl?"*
  * *"Haberleşme şekillerine göre (Ses, Veri, Kripto, Karıştırma Gayda) dağılım tablosunu çıkar."*
* 🛠️ **Çağrılan Tool:** `getComintMacroStats()`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L148-L186`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L148-L186)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  ```sql
  SELECT haberlesme_sekli, COUNT(*) FROM tactical_comint GROUP BY haberlesme_sekli;
  SELECT tip, COUNT(*) FROM tactical_comint GROUP BY tip; -- HF vs VHF
  SELECT lisan, COUNT(*) FROM tactical_comint GROUP BY lisan;
  SELECT teshis_kimlik, COUNT(*) FROM tactical_comint GROUP BY teshis_kimlik;
  ```
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  HF yayınları ufuk ötesi (OTH) stratejik derinlik haberleşmesini, VHF yayınları ise ön hat taktik birliklerinin telsiz çevrimlerini temsil eder. Yabancı dillerin yoğunluğu hasım unsurların etnik ve taktik birlik konuşlanmasını ortaya koyar.

---

### KATEGORİ 2: Kritik Tehdit ve Öncelikli Hedef Sorguları

#### 2.1. Radar (ELINT) Kritik Tehdit Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Şu anki en kritik düşman radar tehditlerini listele."*
  * *"Radar tacizi yapan (kilitlenen/aydınlatan) veya HSS içeren öncelikli hava savunma radarları hangileri?"*
  * *"Acil angajman riski taşıyan düşman radarları nelerdir?"*
  * *"Son tespit edilen en tehlikeli ilk 15 hasım radarını sırala."*
* 🛠️ **Çağrılan Tool:** `queryPriorityThreats(Integer limit)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L150-L169`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L150-L169)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  ```java
  Predicate isEnemy = root.get("teshisKimlik").in(TeshisKimlik.DUSMAN, TeshisKimlik.MUHTEMEL_DUSMAN);
  Predicate isTaciz = cb.isTrue(root.get("taciz"));
  Predicate hasHss = cb.isNotNull(root.get("hss"));
  return cb.or(isTaciz, cb.and(isEnemy, hasHss));
  ```
  *SQL:* `WHERE taciz = TRUE OR (teshis IN ('DUSMAN', 'MUHTEMEL_DUSMAN') AND hss IS NOT NULL) ORDER BY son_tespit_zamani DESC LIMIT 15`
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  * **Aktif Taciz (`taciz = true`):** Radar dairesel arama modundan çıkmış, monopulse kilitlenme ile dar darbe ($PW \le 1.0\,\mu s$) ve yüksek PRF ile dost unsura kilitlenmiştir. NATO Kill-Chain'de **ENGAGE (Ateşleme)** safhasıdır; reaksiyon süresi **< 45 saniyedir**.
  * **HSS Entegrasyonu (`isEnemy AND hasHss`):** Tekil bir sivil/gözetleme radarı değil, S-300/400 veya Pantsir gibi füze bataryasıyla müşterek çalışan entegre hava savunma (IADS) düğümüdür.

---

#### 2.2. Muhabere (COMINT) Kritik Düşman Yayınları
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Öncelikli düşman muhabere yayınlarını ve kritik telsizleri getir."*
  * *"Kriptolu konuşan veya darbe patlama (burst) yapan düşman telsiz ağları var mı?"*
  * *"Telsiz karıştırması uygulayan düşman istasyonları hangileri?"*
  * *"En kritik düşman telsiz tehditlerini listele."*
* 🛠️ **Çağrılan Tool:** `queryPriorityComintThreats(Integer limit)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L188-L207`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L188-L207)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  ```java
  Predicate isDusman = root.get("teshisKimlik").in(TeshisKimlik.DUSMAN, TeshisKimlik.MUHTEMEL_DUSMAN);
  Predicate isKripto = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.KRIPTO);
  Predicate isDarbe = cb.equal(root.get("haberlesmeSekli"), HaberlesmeSekli.DARBE_PATLAMA);
  Predicate isKaristirma = root.get("haberlesmeSekli").in(HaberlesmeSekli.KARISTIRMA_GAYDA, HaberlesmeSekli.KARISTIRMA_GURULTU);
  return cb.or(isDusman, isKripto, isDarbe, isKaristirma);
  ```
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  * **Darbe Patlama (Burst):** DF (Direction Finding) kestiriminden kaçınmak için milisaniyeler mertebesinde paket gönderip telsizi susturan sızma unsurları veya özel kuvvetler timleridir.
  * **Kriptolu Kanallar:** Taktik haberleşme güvenliği (COMSEC) yüksek komuta kademesi telsiz çevrimleridir.
  * **Karıştırma Gayda/Gürültü:** Düşmanın dost birlik frekanslarına karşı taarruz yürüttüğü aktif elektronik harp kanallarıdır.

---

### KATEGORİ 3: Esnek Parametrik Filtreleme (Teknik Zarf Sorguları)

#### 3.1. Çok Kriterli Parametrik Radar Sorgusu
* 💬 **Operatör Doğal Dil Soruları:**
  * *"X-Bandında (8000 - 12000 MHz) çalışan, PRI değeri 200 ile 800 mikro saniye arasındaki düşman radarlarını bul."*
  * *"Kara kaynaklı, PW değeri 2 mikro saniyenin altındaki hedef takip radarları hangileri?"*
  * *"S-Bandında (2000 - 4000 MHz) çalışan erken ihbar radarlarını listele."*
  * *"ET uygulanan veya taciz yapan hava platformu radarları nelerdir?"*
* 🛠️ **Çağrılan Tool:** `queryEmissions(teshis, platformOrtami, veriKaynagi, minFrekans, maxFrekans, minPri, maxPri, minPw, maxPw, minAtp, maxAtp, etUygulaniyor, tacizVar, hssVar, radarGorevi, limit)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L359-L440`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L359-L440)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  Bileşik indeksler (`idx_emi_freq_range`, `idx_emi_pri_range`, `idx_emi_pw_range`) üzerinden dinamik JPA Specification aralık araması yapar.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı (Merrill Skolnik Fiziği):**
  * Dar Darbe ($PW \le 1.5\,\mu s$): Skolnik menzil çözünürlüğü $\Delta R = \frac{c \cdot \tau}{2} \le 225\text{ m}$. Atış kontrol veya hedef takip radarıdır.
  * Geniş Darbe ($PW \ge 10\,\mu s$): Menzil çözünürlüğü $\ge 1.5\text{ km}$. Yalnızca Erken İhbar (Early Warning) radarıdır; doğrudan füze atışı yapamaz.

---

#### 3.2. Çok Kriterli Parametrik Muhabere (COMINT) Sorgusu
* 💬 **Operatör Doğal Dil Soruları:**
  * *"VHF bandında Rusça konuşulan, kriptosuz düşman ses yayınlarını listele."*
  * *"Hareketli telsiz (MTI) emareti olan Arapça haberleşme kayıtlarını getir."*
  * *"HF bandında çalışan dost veri linklerini göster."*
  * *"DMR veya D-Star protokolü kullanan düşman telsiz yayınları hangileri?"*
* 🛠️ **Çağrılan Tool:** `queryComintEmissions(teshis, haberlesmeSekli, tip, lisan, cagriAdi, karsiCagriAdi, minFrekans, maxFrekans, mtiVar, limit)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L525-L600`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L525-L600)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `idx_com_freq_range`, `idx_com_lisan`, `idx_com_protokol` ve `mtiVar` (`mti = true`) indeksli filtreleriyle çok kriterli tarama.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  * **MTI (Moving Target Indicator) Telsiz Emareti:** Telsiz istasyonunun sabit bir mevzide değil, hareket halindeki bir zırhlı araç konvoyunda, hücumbotta veya komuta kontrol aracında olduğunu gösterir.

---

### KATEGORİ 4: Çağrı Adı (Callsign) ve Telsiz Link-Ağ Analizleri

#### 4.1. Link-Pair İkili Temas Adetleri (Kim Kiminle Konuştu?)
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Telsiz ağında kim kiminle konuştu? En çok temas kuran çağrı adı çiftlerini listele."*
  * *"KARTAL-1 çağrı adı ile ATMACA-4 arasında kaç defa görüşme yapılmış?"*
  * *"Son 24 saatte telsiz çiftleri ve konuşma adetleri nedir?"*
  * *"X çağrı adı iken Y karşı çağrı adı kaç defa görüşmüş?"*
  * *"Son 1 saatte/24 saatte kiminle kim ne kadar konuştu?"*
* 🛠️ **Çağrılan Tool:** `getCallsignPairInteractionStats(cagriAdi, karsiCagriAdi, baslangicZamani, bitisZamani, limit)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L644-L714`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L644-L714)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  ```sql
  SELECT cagri_adi, karsi_cagri_adi, COUNT(*), MIN(son_tespit_zamani), MAX(son_tespit_zamani)
  FROM tactical_comint
  WHERE son_tespit_zamani BETWEEN :start AND :end
  GROUP BY cagri_adi, karsi_cagri_adi
  ORDER BY COUNT(*) DESC LIMIT :limit;
  ```
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Haberleşme trafiğindeki emir-komuta matrisini çıkarır. İki çağrı adı arasındaki temas sıklığı, taktik operasyonel işbirliğinin ve ast-üst ilişkisinin göstergesidir.

---

#### 4.2. En Aktif Çağrı Adları Sıralaması
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Sahada en çok konuşan ilk 10 telsiz çağrı adını ve konuşma adetlerini getir."*
  * *"Haberleşme ağında en aktif istasyonlar hangileri?"*
  * *"En çok yayın yapan düşman telsiz çağrı adlarını sırala."*
* 🛠️ **Çağrılan Tool:** `getMostActiveCallsigns(limit, baslangicZamani, bitisZamani)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L716-L751`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L716-L751)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `SELECT cagri_adi, COUNT(*) FROM tactical_comint GROUP BY cagri_adi ORDER BY COUNT(*) DESC LIMIT :limit`
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Ağdaki **Merkez Düğüm İstasyonu (Hub C2 Node)** belirler. En çok konuşan telsiz, genellikle harekât koordinasyonunu yürüten komuta postasıdır.

---

#### 4.3. Telsiz İstasyonu Detaylı Ağ İlişkisi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"KARTAL-1 çağrı adının konuştuğu tüm karşı istasyonları ve link detaylarını analiz et."*
  * *"KARTAL-1 ile ATMACA-4 arasındaki tüm karşılıklı telsiz trafiğini (frekans, kripto, son tespit) dök."*
  * *"Son 24 saatte ŞAHİN-2 istasyonu kimlerle temas sağladı?"*
* 🛠️ **Çağrılan Tool:** `queryComintCallsignNetwork(cagriAdi, karsiCagriAdi, limit)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L753-L820`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L753-L820)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  Hedef çağrı adının geçtiği tüm çift yönlü kayıtlar (`cagri_adi = :x OR karsi_cagri_adi = :x`) listelenir; frekansları, kripto oranları ve protokolleri analiz edilir.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Bir komutanın ya da kritik unsurun hangi taktik birliklerle irtibatta olduğunu haritalandırır.

---

### KATEGORİ 5: Telsiz Trafik Trendi ve Ani Hareketlenme (Surge) Analizi

#### 5.1. Saatlik Trafik Yoğunluğu ve Hareketlenme Tespiti
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Telsiz trafiğinde son 24 saatte bir hareketlenme veya ani artış (surge) var mı?"*
  * *"BOZKURT-9 çağrı adının saatlik trafik yoğunluk grafiği ve trendi nasıl?"*
  * *"Belirli bir frekansta ya da sahada son saatlerde konuşma trafiği neden arttı?"*
  * *"Harekât öncesi telsiz sessizliği veya ani trafik patlaması yaşandı mı?"*
* 🛠️ **Çağrılan Tool:** `getComintTrafficTrend(cagriAdi, zamanAraligi, limit)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L822-L895`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L822-L895)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `date_trunc('hour', son_tespit_zamani)` ile saatlik histogram çıkarılır. Standart sapma $\ge 2.5\sigma$ olan zaman dilimleri "SURGE / ANİ HAREKETLENME" olarak işaretlenir.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  1. **Telsiz Sessizliği (Radio Silence):** Ani trafik düşüşü yaklaşan bir taarruzun son hazırlık safhasıdır.
  2. **Trafik Patlaması (Traffic Surge):** Harekât başlangıç emrinin (H-Hour) verildiğini ve birliklerin angajmana girdiğini gösterir.

---

### KATEGORİ 6: Elektronik Taarruz (ET) ve Karıştırma/Taciz Analizleri

#### 6.1. Detaylı Karıştırma ve Hudut Tacizi Raporu
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Elektronik Taarruz (ET) etkinlik durumunu ve karıştırma başarı oranını raporla."*
  * *"Susturulan (ET ile bastırılan) düşman radarları hangileri?"*
  * *"Hudut hattında taciz uçuşu yapan unsurlar ve karıştırma uygulayan hedefler kimler?"*
  * *"Devam eden aktif karıştırma ve başarı oranımız nedir?"*
* 🛠️ **Çağrılan Tool:** `getDetailedJammingAndHarassmentAnalysis()`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1067-L1120`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1067-L1120)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  $$\text{Karıştırma Başarı Oranı} = \frac{\text{Susturulan (SUSTURDU) Sayısı}}{\text{Toplam ET Uygulanan}} \times 100$$
  Taciz yapan hedeflerin genlik (dBm), frekans ve görev dağılımı listelenir.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Dost Elektronik Taarruz unsurlarının düşman radar ekranlarını karartma (Soft-Kill) başarısını teyit eder.

---

#### 6.2. Radar Elektronik Taarruz Operasyonel Raporu
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Elektronik Taarruz (ET) uygulanan, susturulan veya hudut tacizi yapan hedeflerin operasyonel durum raporunu getir."*
  * *"Hangi radarlara elektronik taarruz uygulanıyor?"*
* 🛠️ **Çağrılan Tool:** `getElectronicAttackReport()`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L488-L506`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L488-L506)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `WHERE et_uygulama_durumu IN ('UYGULANIYOR', 'SUSTURDU') OR taciz = TRUE` kayıtlarını operasyonel durum kartları halinde listeler.

---

#### 6.3. Telsiz Karıştırma ve Haberleşme Güvenliği (COMSEC) Raporu
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Telsiz karıştırma (gayda veya gürültü) tespit edilen frekanslar ve hedefler neler?"*
  * *"Haberleşme güvenliği (COMSEC) ve taktik telsiz emarelerini analiz et."*
  * *"Darbe patlama (burst), kriptolu görüşmeler ve hareketli telsiz (MTI) durumu nedir?"*
* 🛠️ **Çağrılan Tool:** `getComsecAndTransmissionTactics()`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L1140-L1181`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L1140-L1181)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  Kriptolu görüşmeler, darbe patlama (burst), telsiz karıştırma (gayda ve gürültü) ve MTI emareleri tek raporda toplanır.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Taktik telsiz sahasındaki elektronik harp baskısını ve düşmanın TRANSEC/COMSEC disiplinini gösterir.

---

### KATEGORİ 7: Hiyerarşi & LOB Doktrini (Çocuklu/Çocuksuz Elips vs Başıboş LOB)

#### 7.1. Radar (ELINT) Hiyerarşi ve LOB Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Radar kestirim elipslerinden hangileri çocuklu, hangileri çocuksuz?"*
  * *"Henüz bir kestirime dahil olmamış başıboş (stray) radar LOB'larını listele."*
  * *"Son 1 saatte/24 saatte tespit edilen radar kestirimleri ve bağlı LOB'lar nelerdir?"*
  * *"Doğrudan/harici kestirimler ile algılayıcı kestirimleri arasındaki oran nedir?"*
* 🛠️ **Çağrılan Tool:** `analyzeRadarHierarchy(zaman, tip, durum)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1227-L1310`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1227-L1310)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  * **Çocuklu Elips (Childed):** `kayit_tipi = 'KESTIRIM_ELIPS'` olup, altında en az 1 LOB bulunan (`EXISTS (SELECT 1 FROM tactical_emissions WHERE kestirim_id = e.id)`).
  * **Çocuksuz Elips (Childless / Doğrudan):** Altında bağlı LOB bulunmayan doğrudan kestirimler.
  * **Bağlı LOB:** `kayit_tipi = 'HAM_LOB' AND kestirim_id IS NOT NULL`.
  * **Başıboş LOB (Stray):** `kayit_tipi = 'HAM_LOB' AND kestirim_id IS NULL`.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  * **Çocuklu Elips:** Çoklu sensörün açı kesişimiyle hesaplanmış yüksek güvenilirlikli hedeftir.
  * **Çocuksuz Elips:** Link-16/Link-22 veya JREAP üzerinden dış karargâhtan gelen ya da tekil gelişmiş sensörün (TDOA/FDOA uydusu) doğrudan hesapladığı kestirimdir.
  * **Başıboş LOB:** Erken ihbar aşamasında diğer sensörlerin göremediği kör nokta kerterizidir; erken uyarı için hayati önemdedir.

---

#### 7.2. Muhabere (COMINT) Hiyerarşi ve LOB Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Son 1 saatte tespit edilen muhabere elipsleri ve bağlı LOB'ların dökümünü ver."*
  * *"Bugün kaydedilen başıboş telsiz kerteriz hatları hangi yöne bakıyor?"*
  * *"Hangi telsiz kestirimlerinin altında kerteriz hattı (LOB) var veya yok?"*
  * *"Kestirime girmemiş tekil telsiz kerteriz hatları nelerdir?"*
* 🛠️ **Çağrılan Tool:** `analyzeComintHierarchy(zaman, tip, durum)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L1183-L1265`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L1183-L1265)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  Telsiz kestirimleri ve telsiz yön bulma (DF) hatları zaman kısıtı (`SON_1_SAAT`, `SON_24_SAAT`, `BUGUN`) ile taranır.

---

### KATEGORİ 8: Çok Kaynaklı Mekânsal Füzyon (Cross-Domain Fusion: Radar + COMINT)

#### 8.1. CEP Hata Elipsi Örtüşmesi ve Ortak Mevzi Tespiti
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Radar ve telsiz emisyonları arasında coğrafi korelasyon ve çok kaynaklı füzyon analizi yap."*
  * *"Birbirine 5 km mesafede bulunan hava savunma radarı ve eşlik eden telsiz komuta merkezlerini birleştir."*
  * *"Hata elipsleri (CEP) geometrik olarak kesişen ve ortak mevzilenmiş radar/telsiz çiftleri hangileri?"*
* 🛠️ **Çağrılan Tool:** `getCrossDomainCorrelations(Double maxDistanceKm, Integer limit)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L945-L1060`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L945-L1060)
* ⚙️ **Koddaki Filtreleme ve Geometrik Hesaplama:**
  Radar elipsi ($a_r, b_r, \theta_r$) ile telsiz elipsi ($a_c, b_c, \theta_c$) arasında `GeoUtils.calculateEllipseSpatialRelation` hesaplanır:
  1. **Çeperden Çepere Mesafe:** Minimum sınır mesafesi $\le \text{thresholdKm}$.
  2. **Geometrik Kesişim (Overlap):** İki elips alanının örtüşmesi.
  3. **Mahalanobis $\chi^2$ Testi:** %95 istatistiksel güvenle aynı mevzi doğrulaması.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Bir mevzide hem atış kontrol radarı (ELINT) hem de VHF komuta telsizi (COMINT) varsa, burası bağımsız bir sensör değil **Entegre Hava Savunma Bataryası & Ateş İdare Komuta Merkezi (IADS C2 Site)** olarak teşhis edilir.

---

#### 8.2. Tekil İki Emisyon Arasında Hedefli Füzyon
* 💬 **Operatör Doğal Dil Soruları:**
  * *"RAD-0012 radar emisyonu ile COM-0045 telsiz düğümü arasında mekânsal korelasyon ve füzyon analizi yap."*
  * *"RAD-0012 ile COM-0045 aynı mevzide mi konuşlu?"*
* 🛠️ **Çağrılan Tool:** `fuseRadarAndComintNodes(radarId, comintId, maxDistanceKm)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L900-L944`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L900-L944)
* ⚙️ **Koddaki Filtreleme ve Mantık:**
  Verilen iki ID'nin veritabanından koordinat ve yarı eksenleri çekilir; aralarındaki merkez ve çeper mesafesi ile örtüşme metre cinsinden hesaplanır.

---

### KATEGORİ 9: Dalga Şekli Çevikliği (Agility) ve Elektronik Muharebe Düzeni (EOB)

#### 9.1. Teknolojik Dalga Şekli Çevikliği (Agility Tier)
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Sahadaki radarların frekans atlama ve darbe çeviklik seviyelerini (Agility Tier) analiz et."*
  * *"En gelişmiş dalga şekli çevikliğine sahip Tier-1 düşman radarları hangileri?"*
  * *"Frekans atlamalı veya darbe jitter'lı çalışan radarlar nelerdir?"*
* 🛠️ **Çağrılan Tool:** `analyzeRadarWaveformAgility()`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1122-L1180`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1122-L1180)
* ⚙️ **Koddaki Filtreleme ve Mantık:**
  * **Tier-1 (Gelişmiş Çeviklik):** Frekans Atlama (`agile_hop = true`) + Darbe Jitter/Stagger (`pri_modulation = STAGGER/JITTER`) + Darbe Sıkıştırma (Chirp/Barker).
  * **Tier-2 (Orta Seviye):** Yalnızca PRI Stagger veya Darbe Sıkıştırma var, frekans sabit.
  * **Tier-3 (Konvansiyonel):** Sabit frekans, sabit PRI (klasik gürültü ile kolayca karıştırılabilir).
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Tier-1 radarlar klasik gürültü karıştırmasından (Barrage Noise) etkilenmez. Bunlara karşı mutlaka **DRFM (Digital Radio Frequency Memory)** tabanlı RGPO/VGPO aldatma teknikleri uygulanmalıdır.

---

#### 9.2. Elektronik Muharebe Düzeni (EOB) Hiyerarşisi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Elektronik Muharebe Düzeni (EOB) özetini ve HSS batarya yapısını çıkar."*
  * *"Radar görev dağılımı, platform ortamları ve hava savunma mimarisi nedir?"*
  * *"Sahadaki erken ihbar, hedef takip ve atış kontrol radarlarının hiyerarşik düzeni nasıl?"*
* 🛠️ **Çağrılan Tool:** `getElectronicOrderOfBattleSummary()`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1183-L1225`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1183-L1225)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  Radarları görevlerine göre (Erken İhbar, Hedef Takip, Füze Güdüm, Atış Kontrol, Deniz Gözetleme) ve bağlı oldukları HSS bataryalarına (`hss`) göre ağaç yapısında gruplar.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Düşmanın katmanlı hava savunma şemsiyesinin sınırlarını ve derinliğini ortaya koyar.

---

### KATEGORİ 10: Derinlemesine Tekil Emisyon Teknik Kartları

#### 10.1. Tekil Radar Emisyon Kartı
* 💬 **Operatör Doğal Dil Soruları:**
  * *"RAD-0005 ID'li radarın tüm teknik parametrelerini, frekans ve PRI listesini getir."*
  * *"RAD-0010 radarının anten tarama deseni (ATP), operatör notu ve onay durumunu göster."*
* 🛠️ **Çağrılan Tool:** `getEmissionTechnicalDetails(emissionId)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L508-L560`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L508-L560)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `findById("RAD-0005")` ile birincil anahtar üzerinden 37 parametrenin tamamı ve alt ilişki tabloları (Frekans listesi, PRI adımları, ATP fazları) $< 1\text{ ms}$ sürede çekilir.

---

#### 10.2. Tekil Muhabere (COMINT) Emisyon Kartı
* 💬 **Operatör Doğal Dil Soruları:**
  * *"COM-0018 ID'li muhabere kaydının operatör notunu, kripto tipini ve çağrı adlarını göster."*
  * *"COM-0022 telsiz yayınının tüm 30 parametresini dök."*
* 🛠️ **Çağrılan Tool:** `getComintEmissionDetails(comintId)`
* 📍 **Kaynak Kod Konumu:** [`ComintEmissionTools.java:L897-L950`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L897-L950)
* ⚙️ **Koddaki Filtreleme ve SQL Mantığı:**
  `findById("COM-0018")` ile 30 muhabere parametresi, çoklu frekans listesi ve istihbarat notları döner.

---

### KATEGORİ 11: Radar Spektrum Doluluk & Sabit Frekans Aralıkları Yoğunluk Analizi

#### 11.1. Sabit Frekans Dilimleri (50/100/200 MHz) Doluluk ve Örtüşme (Union) Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Düşman radarlarının sabit frekans dilimlerindeki spektrum doluluk ve kullanım yoğunluğunu tablo ve bar grafiği olarak analiz et."*
  * *"Son 24 saatte düşman radarları spektrumun hangi bantlarını (MHz) ne oranda işgal etmiş?"*
  * *"Dost ve düşman radarlarının frekans spektrumundaki çakışma ve kullanım yoğunluğu nedir?"*
  * *"Radar frekans doluluk tablosunu 50 MHz dilimler halinde çıkar."*
* 🛠️ **Çağrılan Tool:** `getRadarSpectrumOccupancy(String timeRange, String teshisKimlik)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1465-L1510`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1465-L1510)
* ⚙️ **Koddaki Filtreleme ve Matematiksel Mantık:**
  * Sabit dilimler (örn: 100-150 MHz, 150-200 MHz... 11950-12000 MHz) üzerinde radarların min-max frekans aralıklarının matematiksel birleşimi (`Interval Union`) hesaplanır.
  * Mükerrer emisyonların aynı aralığı çift sayması engellenir; dilimin toplam MHz genişliği üzerinden net işgal yüzdesi (`Occupancy %`) ve emisyon sayısı bulunur.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Elektronik Destek (ED) ve Taarruz (ET) planlamasında "Düşmanın en çok yığıldığı kritik frekans kanalları hangileridir?" sorusunun yanıtıdır. Karıştırma kaynaklarının (Jammer) hangi bantlara tahsis edileceğine karar verilir.

---

### KATEGORİ 12: Kategorik Dağılım ve Taktik Pasta/Halka/Gül Grafiği Analizi

#### 12.1. 11+ Kriter Üzerinden Taktik Pasta/Halka/Gül Grafiği
* 💬 **Operatör Doğal Dil Soruları:**
  * *"Radarların görev tiplerine (Arama, Takip, Atış Kontrol) göre dağılım pastasını çıkar."*
  * *"Telsiz lisan dağılımını (Rusça, Arapça, Yunanca vb.) halka grafik olarak analiz et."*
  * *"Düşman radarlarının polarizasyon (Yatay, Dikey, Dairesel) veya platform dağılımı nedir?"*
  * *"Telsiz haberleşme şekillerinin oranlarını gül grafiğinde (Nightingale Rose) göster."*
* 🛠️ **Çağrılan Tool:** `getTacticalDistribution(String source, String criterion, String timeRange, Integer limit)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java:L1425-L1445`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1425-L1445)
* ⚙️ **Koddaki Filtreleme ve Mantık:**
  `source` (`RADAR` / `COMINT`) ve `criterion` (`TESHIS`, `GOREV`, `POLARIZASYON`, `MODULASYON`, `PLATFORM`, `HEDEF_YER`, `CAGRI_ADI`, `LISAN`, `HABERLESME_SEKLI`, `BANT`) parametrelerine göre veritabanında tek geçişli gruplama ve yüzde hesabı yapılır.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Kuvvet konuşlanması ve tehdit niteliğinin görsel brifing özetidir; sağ panelde otomatik olarak açılan interaktif pie-simple grafi�* ⚙️ Koddaki Filtreleme ve Puanlama:
  RF İmzası (%45) + Zaman (%20) + Taktik/Coğrafya (%35) formülüyle 100 puan üzerinden benzerlik skoru hesaplar; Top-K sıralaması yapar.

---

### KATEGORİ 15: Haftanın Günlerine Göre LOB ve Kestirim Dağılımı (Örümcek Ağı / Radar Grafiği)

#### 15.1. Günlük LOB ve Kestirim Dağılımı Örümcek Ağı Analizi
* 💬 **Operatör Doğal Dil Soruları:**
  * *"haftanın günlerine göre radar ya da muhabere kestirim/ LOB sayısını grafiğini ver"*
  * *"haftanın günlerine göre LOB tespit sayısı örümcek ağı grafiği"*
  * *"günlere göre radar ve telsiz yayın dağılımını radar grafiğinde göster"*
  * *"haftalık LOB ve kestirim elipsi dağılımı"*
  * *"pazartesiden pazara kadar gün bazında radar ve muhabere yayın yoğunlukları"*
* 🛠️ **Çağrılan Tool:** `getWeekdayEmissionDistribution(String source, String recordType, String timeRange)`
* 📍 **Kaynak Kod Konumu:** [`RadarEmissionTools.java`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java) & [`TacticalWeekdayRadarService.java`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/service/TacticalWeekdayRadarService.java)
* ⚙️ **Koddaki Filtreleme ve Mantık:**
  PostgreSQL üzerinde `EXTRACT(ISODOW FROM COALESCE(ilk_tespit_zamani, son_tespit_zamani))::int` fonksiyonuyla 1 (Pazartesi) ile 7 (Pazar) aralığındaki günler indekslenir. Tek geçişte koşullu toplama (`COUNT(*) FILTER (WHERE kayit_tipi = 'HAM_LOB')` ve `COUNT(*) FILTER (WHERE kayit_tipi = 'KESTIRIM_ELIPS')`) yapılarak hem radar hem de muhabere için 7 günlük dizi üretilir.
* 🎖️ **Askeri Doktrin ve Taktik Anlamı:**
  Düşmanın haftalık harekât ritmini (tempo), tatbikat günlerini, hafta içi/hafta sonu telsiz disiplinini ve hava savunma radarlarını hangi günlerde aktif hale getirdiğini 7 kollu örümcek ağı / radar grafiğinde periyodik olarak analiz eder.
* 🖥️ **Arayüz (Frontend) Widget Entegrasyonu:**
  Operatör bu soruyu sorduğunda Copilot yanıtıyla birlikte sağ analiz panelinde **`WEEKDAY_RADAR` (Örümcek Ağı Grafiği)** widget'ını otomatik olarak açar. Operatör menü çubuğundaki **`🕸️ Haftalık LOB Dağılımı`** butonundan da bağımsız olarak filtreleri (Tümü / Radar / Muhabere, Tümü / Sadece LOB / Sadece Kestirim, Zaman Filtreleri) değiştirerek grafiği etkileşimli inceleyebilir.

---

## 3. HIZLI REFERANS MATRİSİ (TAM LİSTE)

| # | Kategori | Tipik Operatör Sorusu | Tetiklenen Tool | Filtre / Doktrin Özeti | Kod Konumu |
|---|---|---|---|---|---|
| 1 | **Kritik Radar Tehdidi** | *"En kritik düşman radarlarını listele"* | `queryPriorityThreats` | `taciz=true OR (düşman AND HSS)` | [`RadarEmissionTools.java:L150`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L150) |
| 2 | **Kritik Telsiz Tehdidi** | *"Kritik düşman telsiz ağlarını getir"* | `queryPriorityComintThreats` | `Düşman OR Kripto OR Burst OR Gayda` | [`ComintEmissionTools.java:L188`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L188) |
| 3 | **Radar Makro Durum** | *"Genel radar istatistiklerini özetle"* | `getTacticalMacroStats` | Teşhis, Hiyerarşi, ET, Platform gruplama | [`RadarEmissionTools.java:L99`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L99) |
| 4 | **Telsiz Makro Durum** | *"Telsiz trafiği özetini çıkar"* | `getComintMacroStats` | Kripto, Lisan, HF/VHF, Hiyerarşi | [`ComintEmissionTools.java:L148`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L148) |
| 5 | **Parametrik Radar** | *"X-Bandında dar PW'li radarları bul"* | `queryEmissions` | Frekans, PRI, PW, ATP zarf taraması | [`RadarEmissionTools.java:L359`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L359) |
| 6 | **Parametrik Telsiz** | *"VHF bandında Rusça ses yayınları"* | `queryComintEmissions` | Lisan, Protokol, Modülasyon, MTI | [`ComintEmissionTools.java:L525`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L525) |
| 7 | **Link-Pair Analizi** | *"Kim kiminle konuştu? Temas adetleri"* | `getCallsignPairInteractionStats`| `GROUP BY cagri, karsi_cagri` (Isı Haritası) | [`ComintEmissionTools.java:L644`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L644) |
| 8 | **En Aktif Çağrı Adı** | *"En çok konuşan telsiz istasyonları"* | `getMostActiveCallsigns` | `GROUP BY cagri_adi ORDER BY COUNT` | [`ComintEmissionTools.java:L716`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L716) |
| 9 | **Çağrı Adı Ağ Analizi**| *"KARTAL-1'in tüm temas kanalları"* | `queryComintCallsignNetwork` | Tek istasyonun çift yönlü link haritası | [`ComintEmissionTools.java:L753`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L753) |
| 10 | **Trafik Trendi / Surge**| *"Telsiz trafiğinde ani artış var mı?"*| `getComintTrafficTrend` | Saatlik histogram + $2.5\sigma$ sapma | [`ComintEmissionTools.java:L822`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L822) |
| 11 | **Detaylı Karıştırma** | *"ET etkinlik ve karıştırma durumu"* | `getDetailedJammingAndHarassmentAnalysis`| Susturulan / ET uygulanan oranları | [`RadarEmissionTools.java:L1067`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1067) |
| 12 | **Radar ET Durumu** | *"ET uygulanan radarların raporu"* | `getElectronicAttackReport` | `et_uygulama_durumu != 'YOK'` | [`RadarEmissionTools.java:L488`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L488) |
| 13 | **Telsiz COMSEC Raporu**| *"Telsiz karıştırma ve burst durumu"* | `getComsecAndTransmissionTactics` | Kripto, Burst, Gayda/Gürültü, MTI | [`ComintEmissionTools.java:L1140`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L1140) |
| 14 | **Radar Hiyerarşisi** | *"Çocuklu elipsler ve başıboş LOB'lar"*| `analyzeRadarHierarchy` | `kestirim_id IS NULL / NOT NULL` | [`RadarEmissionTools.java:L1227`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1227) |
| 15 | **Telsiz Hiyerarşisi** | *"Başıboş telsiz LOB'ları hangileri?"* | `analyzeComintHierarchy` | Kestirimsiz telsiz DF hatları tespiti | [`ComintEmissionTools.java:L1183`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L1183) |
| 16 | **Çok Kaynaklı Füzyon**| *"Radar ve telsiz ortak mevzi analizi"* | `getCrossDomainCorrelations` | Geometrik CEP örtüşme + Mahalanobis | [`RadarEmissionTools.java:L945`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L945) |
| 17 | **Hedefli Füzyon** | *"RAD-0012 ile COM-0045 ortak mı?"* | `fuseRadarAndComintNodes` | İki ID arasında çeper/merkez mesafe | [`RadarEmissionTools.java:L900`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L900) |
| 18 | **Dalga Şekli Çevikliği**| *"Radarların Agility Tier seviyeleri"* | `analyzeRadarWaveformAgility` | Hop + Stagger + Darbe Sıkıştırma | [`RadarEmissionTools.java:L1122`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1122) |
| 19 | **EOB Hiyerarşisi** | *"Elektronik Muharebe Düzeni çıkar"* | `getElectronicOrderOfBattleSummary` | Görev + Platform + HSS hiyerarşisi | [`RadarEmissionTools.java:L1183`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1183) |
| 20 | **Tekil Radar Kartı** | *"RAD-0005 radarının tüm detayları"* | `getEmissionTechnicalDetails` | PK üzerinden 37 parametre ve listeler | [`RadarEmissionTools.java:L508`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L508) |
| 21 | **Tekil Telsiz Kartı** | *"COM-0018 telsizinin tüm detayları"* | `getComintEmissionDetails` | PK üzerinden 30 parametre ve detaylar | [`ComintEmissionTools.java:L897`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/ComintEmissionTools.java#L897) |
| 22 | **Spektrum Doluluk** | *"Düşman radarlarının frekans doluluk oranı"* | `getRadarSpectrumOccupancy` | 50/100/200 MHz dilimlerde örtüşme (Union) | [`RadarEmissionTools.java:L1467`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1467) |
| 23 | **Taktik Dağılım** | *"Radar görev tiplerinin pasta dağılımı"* | `getTacticalDistribution` | 11+ kriterde pasta, halka, gül grafiği | [`RadarEmissionTools.java:L1430`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1430) |
| 24 | **Dalga Formu Çizelgesi**| *"SP048 radarının dalga formu değişimi"* | `getWaveformTimeline` | Frekans, PRI, PW ve Genlik zaman serisi | [`RadarEmissionTools.java:L1449`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L1449) |
| 25 | **1-to-1 Karşılaştırma** | *"RAD-0012 ile RAD-0045 radarını karşılaştır"*| `compareRadarEmissions` | 1D IoU ile Zaman, RF, PRI, PW analizi | [`RadarEmissionTools.java:L629`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L629) |
| 26 | **1-to-N Benzerlik** | *"Bu radara sahada en çok benzeyen ilk 5 hedef"*| `findSimilarRadarEmissions` | 100 puanlık çok kriterli matris & Top-K | [`RadarEmissionTools.java:L640`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java#L640) |
| 27 | **Haftalık LOB & Kestirim Dağılımı** | *"Haftanın günlerine göre radar ya da muhabere LOB sayısını grafiğini ver"* | `getWeekdayEmissionDistribution` | 7 kollu Apache ECharts Örümcek Ağı / Radar grafiği; tek geçişli ISODOW gruplaması | [`RadarEmissionTools.java`](file:///c:/Users/Aselsan/Desktop/EWSpringAI_v1/springAIDeneme3/src/main/java/com/c2/ew/agent/tools/RadarEmissionTools.java) |
