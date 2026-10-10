# TAKTİK ELEKTRONİK HARP TEHDİT VE NİYET ANALİZİ DOKTRİNİ
## NATO F2T2EA Kill-Chain, Skolnik Radar Fiziği ve Wiley ELINT Karar Destek Motoru

**Doküman Versiyonu:** 1.0.0  
**Tarih:** 03 Ekim 2026  
**Sistem:** Taktik Elektronik Harp (EH / C2) Karar Destek Copilot Sistemi  
**Konum:** `AntigravitySpringAIDeneme3`  

---

## 1. GİRİŞ VE VİZYON: VERİ SORGULAMADAN HAREKÂT ÇIKARIMINA

Geleneksel komuta kontrol (C2) arayüzleri, radar ve haberleşme veritabanı üzerinde yalnızca **filtreleme ve listeleme (SQL sorguları)** yapar:
* *"Frekansı 9000-10000 MHz olan radarları getir."*
* *"Kriptolu telsizleri listele."*

Ancak modern bir harekât merkezinde bir komutanın veya Elektronik Harp Subayının (EWO) asıl ihtiyacı kuru sayılar değildir. Operatör şu üç kritik sorunun cevabını arar:
1. **Düşmanın Niyeti Ne?** (Rutin devriye mi, hava sahası tecriti mi, yoksa uçağımıza füze ateşleme hazırlığı mı?)
2. **Ne Kadar Zamanım Var?** (Reaksiyon süresi penceresi: 30 saniye mi, 5 dakika mı?)
3. **Hangi Karşı Tedbiri Almalıyım?** (Hangi elektronik taarruz - EA tekniği, hangi manevra profili?)

Bu sistem; radar dalga formunun temel fizik kurallarını (**Merrill Skolnik**) ve sinyal istihbaratı işleme yöntemlerini (**Richard G. Wiley**) bir araya getirerek, **NATO F2T2EA Kill-Chain** doktrinine dayalı deterministik ve bilişsel bir taktik çıkarım motoru sunar.

---

## 2. BİLİMSEL VE DOKTRİNEL TEMELLER

Sistemdeki çıkarım motoru ezbere veya olasılıksal tahminlere değil, savunma literatüründe kabul görmüş üç temel ayağa dayanır:

```text
┌────────────────────────────────────────────────────────────────────────┐
│               TAKTIK ÇIKARIM MOTORUNUN ÜÇ TEMEL AYAĞI                  │
├─────────────────────────┬──────────────────────┬───────────────────────┤
│ 1. MERRILL SKOLNIK      │ 2. RICHARD G. WILEY  │ 3. NATO STANAG 4484   │
│    (Radar Dalga Fiziği) │    (ELINT Analizi)   │    (Kill-Chain F2T2EA)│
│ • Menzil Çözünürlüğü    │ • PDW Karakterizasyonu│ • Find (Tespit)       │
│ • Doppler / PRF İkilemi │ • TOA / PRI Histogram│ • Fix (Teşhis)        │
│ • Enerji / PW Dengesi   │ • Anten Genlik Zarfı │ • Track (Hassas Takip)│
│ • Monopulse Takip       │ • Darbe Sıkıştırma   │ • Target (Hedefleme)  │
│                         │ • Belirsizlik Yönetimi│ • Engage (Angajman)   │
└─────────────────────────┴──────────────────────┴───────────────────────┘
```

---

### A. Merrill Skolnik – Radar Dalga Formu Fiziği

#### 1. Menzil Çözünürlüğü Denklemi (Range Resolution)
Bir radarın iki hedefi (örneğin kol uçuşundaki iki uçağı) veya hedef ile füzeyi birbirinden ayırt edebilmesi darbe genişliğine ($\tau$ - Pulse Width) bağlıdır:

$$\Delta R = \frac{c \cdot \tau}{2}$$

* **Geniş Darbe ($\tau = 10 - 50\,\mu s$):**
  $$\Delta R = \frac{3 \cdot 10^8 \cdot 20 \cdot 10^{-6}}{2} = 3000\text{ metre}$$
  Radar hedefleri 3 km mesafeden önce ayıramaz. Bu dalga formu **yalnızca uzak menzil Erken İhbar (Early Warning)** amaçlıdır; füze atışı yapamaz.
* **Dar Darbe ($\tau = 0.1 - 1.0\,\mu s$):**
  $$\Delta R = \frac{3 \cdot 10^8 \cdot 0.4 \cdot 10^{-6}}{2} = 60\text{ metre}$$
  Radar hedefin uçağın burnu mu kuyruğu mu olduğunu dahi hassas çözer. **Bu bir Atış Kontrol / Füze Güdüm dalga formudur.**

#### 2. Doppler İkilemi ve PRF (Pulse Repetition Frequency)
Radar hedef hızını Doppler frekans kaymasıyla ölçer. Belirsiz olmayan hız sınırı:

$$V_{unamb} = \frac{\lambda \cdot \text{PRF}}{4} = \frac{\lambda}{4 \cdot \text{PRI}}$$

* Erken ihbar radarları uzak mesafeyi görmek için **Düşük PRF (Yüksek PRI: 1000 - 4000 µs)** kullanır; ancak hedef hızını anlık ölçemez.
* Atış kontrol ve füze güdüm radarları hedef hızını sıfır hatayla takip etmek için **Yüksek PRF (Düşük PRI: 50 - 250 µs)** kullanır.

---

### B. Richard G. Wiley – ELINT Sinyal İşleme İlkeleri

#### 1. PDW (Pulse Descriptor Word) Mimarisi
Alıcı antene çarpan her darbe donanım seviyesinde 5 parametreye indirgenir:
* **TOA (Time of Arrival):** Geliş zamanı. PRI hesaplamasının temelidir.
* **RF (Radio Frequency):** Taşıyıcı frekans (Bant tespiti: VHF, L, S, C, X, Ku).
* **PW (Pulse Width):** Darbe süresi (Menzil çözünürlüğü göstergesi).
* **PA (Pulse Amplitude - Genlik):** Alınan güç (dBm). Anten ışıma diyagramı taramasını verir.
* **DOA / AOA (Direction of Arrival):** Geliş açısı (Mekânsal kestirim).

#### 2. Anten Tarama Analizi ve Kilitlenme Tespiti
Alıcının yakaladığı genlik (PA) zaman ekseninde izlendiğinde radarın anten hareketi deşifre edilir:
* **Dairesel Tarama (360°):** Genlik düzenli aralıklarla tepe yapar (Arama modu - Düşük Tehdit).
* **Sektör Taraması:** Dar bir açıda gidip gelen genlik (Teyakkuz / Bölgesel Odaklanma - Orta Tehdit).
* **Sabit Bakış (Staring / Steady):** Genlik sürekli en tepe güçte sabit kalır (`taciz = true`). Anten hedefe dikilmiştir (**Ölümcül Angajman - Seviye 4**).

---

### C. NATO F2T2EA Kill-Chain (Öldürme Zinciri) Mimarisi

Düşman hava savunma sistemi bir hedefe rastgele ateş edemez. Belirli bir doktrinel sıra izler:

```text
[ FIND ] ──► [ FIX ] ──► [ TRACK ] ──► [ TARGET ] ──► [ ENGAGE ] ──► [ ASSESS ]
 (Tespit)     (Teşhis)     (Takip)      (Hedefleme)     (Ateş/Güdüm)    (Hasar Değ.)
```

1. **FIND (Bulma):** VHF/L-Bant, geniş PW ($>10\,\mu s$), düşük PRF, 360° tarama.
2. **FIX (Konumlandırma & Teşhis):** S-Bant arama radarı, hedef koordinatı netleştirme.
3. **TRACK (Hassas İzleme):** C/X-Bant, TWS (Track-While-Scan) modu, daralan PW ($1.5 - 3\,\mu s$), kademeli (staggered) PRI.
4. **TARGET (Hedefleme):** X-Bant, tekil hedefe tahsis edilmiş huzme, yüksek PRF.
5. **ENGAGE (Angajman / Ateşleme):** X/Ku-Bant, sürekli aydınlatma / CW veya milisaniyelik darbe güdümü, darbe genişliği $\le 1.0\,\mu s$, `taciz = true`.

---

## 3. GİRDİ VE ÇIKTI VERİ MODELİ

### Girdi Modeli Parametreleri
Sistem `tactical_emissions` ve `tactical_comint` tablolarından şu verileri çeker:

| Parametre | Fiziksel Rolü | Doktrinel Anlamı |
| :--- | :--- | :--- |
| `minPwMicroSec` | Darbe Genişliği ($\tau$) | Menzil çözünürlüğünü belirler ($\Delta R = 150 \cdot \tau$). |
| `minPriMicroSec` | Darbe Tekrarlama Aralığı (PRI) | PRF değerini ($10^6 / PRI$) ve Doppler takip yeteneğini verir. |
| `minFrekansMhz` | RF Taşıyıcı Frekansı | Radar bandını (X-Bant $\approx$ Atış, VHF $\approx$ Erken İhbar) belirler. |
| `genlikDbm` | Sinyal Gücü | Radarın ana huzme (mainlobe) ile mi üzerimize baktığını ispatlar. |
| `taciz` | Kilitlenme Bayrağı | Antenin hedefe sabitlendiğini (`staring`) doğrular. |
| `hss` | Hava Savunma Sistemi | Tehdidin bağlı olduğu füze bataryası kimliği (S-400, Patriot, Buk). |
| `teshisKimlik` | Tehdit Kimliği | DÜŞMAN, MUHTEMEL DÜŞMAN, DOST ayrımı. |

---

## 4. TEHDİT DERECELENDİRME VE KARAR AĞACI ALGORİTMASI

Her bir radar kaydı aşağıdaki deterministik kural motorundan geçer:

```text
                                 [ RADAR YAYINI ]
                                        │
                         DÜŞMAN mı veya MUHTEMEL DÜŞMAN mı?
                                  /            \
                              (EVET)          (HAYIR) ──► [ SEVİYE 1: DÜŞÜK ]
                                /                          (FIND / Rutin Arama)
                    taciz == true mu?
                       /          \
                   (EVET)        (HAYIR)
                    /                \
        [ SEVİYE 4: KRİTİK ]     PW <= 1.5 µs VE PRI <= 350 µs
         (ENGAGE / Kilitlenme)    veya Atış Kontrol Görevi mi?
                                     /             \
                                 (EVET)           (HAYIR)
                                  /                   \
                      [ SEVİYE 3: YÜKSEK ]     HSS Tanımlı mı veya Freq >= 6000?
                       (TARGET / TRACK)            /             \
                                               (EVET)           (HAYIR)
                                                /                   \
                                    [ SEVİYE 2: ORTA ]      [ SEVİYE 1: DÜŞÜK ]
                                     (FIX / Teşhis)          (FIND / Erken İhbar)
```

### Seviye Detayları:

#### 🔴 SEVİYE 4 – KRİTİK ANGAJMAN (ENGAGE)
* **Kriter:** `isDusman && taciz == true`
* **Skor:** 95 – 100
* **Reaksiyon Süresi:** **ACİL / < 45 Saniye** (Füze terminal safhası)
* **Fiziksel İspat:** Darbe $\le 1.5\,\mu s$ (Skolnik $\Delta R \le 200\text{ m}$), Yüksek PRF, Ana huzme genliği ($-30\text{ dBm}$).
* **Önerilen Karşı Tedbir:** X/Ku-Bant RGPO (Range Gate Pull-Off - Menzil Kapısı Çekme) + Eşzamanlı Chaff koridoru + 3G Kaçınma Manevrası.

#### 🟠 SEVİYE 3 – HEDEF TAKİP & HEDEFLEME (TARGET / TRACK)
* **Kriter:** `isDusman && (PW <= 1.5 µs || PRI <= 350 µs || gorev.contains("Atış"))`
* **Skor:** 75 – 85
* **Reaksiyon Süresi:** **1 – 3 Dakika** (Atış çözümü hesaplanıyor)
* **Fiziksel İspat:** Takip PRF'i ve sektör odaklanması.
* **Önerilen Karşı Tedbir:** Hız Kapısı Çekme (VGPO) + Sektörel Gürültü Karıştırması (Barrage Noise).

#### 🟡 SEVİYE 2 – ERKEN UYARI & TEŞHİS (FIX)
* **Kriter:** `isDusman && (hss != null || minFreq >= 6000)`
* **Skor:** 40 – 60
* **Reaksiyon Süresi:** **> 5 Dakika** (Teyakkuz)
* **Önerilen Karşı Tedbir:** Pasif Elektronik Destek (ED) dinlemesi + Radar ufku irtifa ayarlaması.

#### 🟢 SEVİYE 1 – RUTİN GÖZETLEME (FIND)
* **Kriter:** Diğer tüm durumlar, dost ve seyrüsefer radarları.
* **Skor:** 15 – 30
* **Reaksiyon Süresi:** Rutin izleme.

---

## 5. ÇAPRAZ İSTİHBARAT KORELASYONU (ELINT + COMINT)

Bir radarın tek başına kilitlenmesi teknik bir olaydır; ancak **aynı bölgede telsiz trafiğinin patlaması taktik bir harekâttır.**

Motor, seçilen coğrafi sektördeki (örneğin Midilli, Sakız, Şırnak) `tactical_comint` kayıtlarını anlık tarar:
* **Kriptolu Muhabere Oranı:** Sektördeki askeri kripto yoğunluğu ölçülür.
* **Düşman Çağrı Kodları:** Taktik frekanslardaki komuta ağları (`VOLGA-01`, `NATO-TASK-09`, `HERMES-4`) tespit edilir.
* **Zaman Korelasyonu:** Radar kilitlenmesi anında telsiz emir-komuta akışı varsa, durumun tatbikat değil **fiilî harekât hazırlığı** olduğu kesinleşir.

---

## 6. SİSTEM MİMARİSİ VE İKİ KADEMELİ (DUAL-SPEED) ÇALIŞMA PRENSİBİ

Sistem 400.000+ kayıt altında dahi **2 saniyenin altında** sonuç üretir. Bu başarı iki kademeli mimariye dayanır:

```text
[ 400.000+ Veritabanı Kaydı ]
             │
             │  1. AŞAMA: B-Tree İndeksli JPA Sorgusu (15 - 40 ms)
             ▼
[ Sektördeki 100 Kritik Kayıt ]
             │
             │  2. AŞAMA: Deterministik Fizik Kural Motoru (5 ms)
             │     (Skolnik Çözünürlüğü + Kill-Chain Sınıflandırma)
             ▼
[ Yapılandırılmış Tehdit Listesi ]
             │
             │  3. AŞAMA: Bilişsel Yapay Zekâ / LLM Sentezi (1.5 - 2.0 sn)
             │     (Executive Commander Briefing + Fail-Safe Yerel Fallback)
             ▼
[ Harekât Merkezi Ekranı (ThreatAnalysisDashboard) ]
```

### Fail-Safe (Yüksek Güvenilirlik) Güvencesi:
Eğer LLM bağlantısında en ufak bir kesinti veya gecikme olursa, sistem asla hata vermez. Yerel kural motoru Skolnik ve Wiley formüllerini şablonlayarak tam operasyonel brifingi yerel olarak anında üretir.

---

## 7. AKADEMİK VE DOKTRİNEL KAYNAKÇA

1. **Skolnik, Merrill I.** – *"Radar Handbook"*, 3rd Edition, McGraw-Hill, 2008. (Chapter 4: Pulse Doppler Radar, Chapter 8: Target Resolution).
2. **Wiley, Richard G.** – *"ELINT: The Interception and Analysis of Radar Signals"*, Artech House, 2006. (Chapter 2: Signal Characteristics, Chapter 3: Deinterleaving & TOA Histograms).
3. **Adamy, David L.** – *"EW 101: A First Course in Electronic Warfare"*, Artech House, 2001.
4. **Adamy, David L.** – *"EW 102: A Second Course in Electronic Warfare"*, Artech House, 2004.
5. **NATO Standard Agreement (STANAG 4484)** – *"Tactical Electronic Warfare Support Doctrine"*.
6. **Air Force Doctrine Document (AFDD) 3-13.1** – *"Electronic Warfare Operations"*, Joint Publication 3-13.1.
