---
name: sigint-c2-doctrine
description: >-
  Askeri Elektronik Harp (EH / EW) ve Sinyal İstihbaratı (SIGINT) sistemleri için yetkili bilgi tabanı
  ve doktrin kuralları. ELINT (Radar emisyonları, PRI, PW, MOP, ATP, HSS, ET/Karıştırma, Taciz),
  COMINT (Telsiz ağları, HF/VHF, kriptolu, darbe-patlama, çapraz/diyalog trafiği), Kestirim Elipsi ve
  LOB hiyerarşisi (Çocuklu/Çocuksuz Elips, Bağlı/Başıboş LOB), NATO Taktik Veri Linkleri (Link-16, Link-22, JREAP)
  ve Frekans Spektrum Doluluk Analizi kurallarını kapsar.
---

# Sinyal İstihbaratı (SIGINT) & Elektronik Harp (EH / C2) Doktrin Rehberi

Bu yetenek dokümanı, savunma sanayii ve askeri Komuta Kontrol (C2) karar destek sistemlerinde kullanılan **Sinyal İstihbaratı (SIGINT)** ve **Elektronik Harp (EH / EW)** analizi için bağlayıcı doktrinel standartları, matematiksel temelleri ve taktiksel sınıflandırma kurallarını tanımlar.

---

## 1. Temel Sinyal İstihbaratı (SIGINT) Taksonomisi

```
                       ┌─────────────────────────────────────────┐
                       │       SİNYAL İSTİHBARATI (SIGINT)       │
                       └────────────────────┬────────────────────┘
                                            │
                   ┌────────────────────────┴────────────────────────┐
                   ▼                                                 ▼
      ┌───────────────────────────┐                     ┌───────────────────────────┐
      │       ELINT (RADAR)       │                     │     COMINT (MUHABERE)     │
      │   Haberleşme-Dışı Yayınlar │                     │    Telsiz / Ses / Veri    │
      └───────────────────────────┘                     └───────────────────────────┘
```

### A. ELINT (Elektronik İstihbarat - Radar ve Silah Sistemleri)
Haberleşme harici elektromanyetik yayınları (özellikle radar ve güdüm sistemleri) inceler.
* **Radar Görev Tipleri:**
  * **Erken İhbar (Early Warning):** Geniş alan taraması yapan düşük frekanslı (VHF, UHF, L-Bant) uzun menzilli radarlar.
  * **Arama ve Takip (Target Acquisition / Track):** Orta menzilli, hedef hız ve koordinatını hassaslaştıran sistemler (S-Bant, C-Bant).
  * **Atış Kontrol (Fire Control):** Silah sistemine (namlu veya hava savunma füzesine) hedef atayan yüksek frekanslı (X-Bant, Ku-Bant) dar huzmeli radarlar.
  * **Füze Güdüm (Missile Guidance):** Füzenin hedefe kilitlenmesini sağlayan aydınlatma ve komuta linki yayınları.
  * **Hava Önleme / Uçak Radarı (Airborne Intercept - AI):** Avcı ve bombardıman uçaklarının burun radarları (örn. APG-77, APG-81).
* **Temel Sinyal Parametreleri:**
  * **RF (Radyo Frekansı):** Taşıyıcı sinyalin MHz/GHz cinsinden çalışma aralığı (`minFrekansMhz` - `maxFrekansMhz`).
  * **PRI (Darbe Tekrarlama Aralığı - Pulse Repetition Interval):** Ardışık iki radar darbesi arasındaki süre (mikrosaniye - µs). Sabit (Fixed), kademeli (Staggered) veya rastgele (Jittered) olabilir.
  * **PW (Darbe Genişliği - Pulse Width):** Tek bir darbenin süresi (µs).
  * **MOP (Darbe İçi Modülasyon - Modulation on Pulse):** Doğrusal frekans modülasyonu (Chirp), Barker kodlaması, Çok Fazlı (Polyphase P1-P4), Frekans Atlamalı.
  * **ATP (Anten Tarama Periyodu - Antenna Scan Period):** Radar anteninin 360 derecelik bir tam turu tamamlama süresi (saniye).
* **Elektronik Taarruz (ET) ve Taciz:**
  * **ET (Karıştırma / Jamming):** Gürültü karıştırması (Noise Jamming), Aldatma karıştırması (Deception Jamming), Tekrarlayıcı (DRFM Repeater).
  * **Taciz (Radar Kilidi / Aydınlatma):** Düşman atış kontrol radarının mühimmat ateşlemeden dost unsurlara kilitlenerek aydınlatması (angajman tehdidi).

### B. COMINT (Muhabere İstihbaratı - Telsiz ve Taktik Ağlar)
Askeri haberleşme ağlarını ve telsiz emisyonlarını inceler.
* **Frekans Bantları:**
  * **HF (Yüksek Frekans, 3–30 MHz):** İyonosferik yansıma ile ufuk ötesi (BLOS) kıtalararası stratejik haberleşme.
  * **VHF (Çok Yüksek Frekans, 30–300 MHz):** Görüş hattı (LOS) taktik kara birlikleri, hava-yer haberleşmesi, deniz emniyet kanalları.
  * **UHF (Ultra Yüksek Frekans, 300–3000 MHz):** Taktik veri linkleri, İHA telemetrisi ve askeri uydu haberleşmesi (SATCOM).
* **Yayın Karakteristikleri:**
  * **Ses (Voice):** Açık analog veya dijital ses trafiği.
  * **Kriptolu (Crypto):** Şifrelenmiş veri veya ses akışı.
  * **Darbe-Patlama (Burst / Frequency Hopping):** Düşük Tespit Edilme Olasılığı (LPI/LPD) hedefleyen, saliseler süren çok yüksek hızlı veri paketleri.
* **Trafik Örüntüleri:**
  * **Birebir (1-to-1):** İki çağrı kodu (komutan-unsur) arasındaki direkt taktiksel hat.
  * **Ağ (Broadcast / Net):** Tek bir komuta merkezinin tüm taktik ağdaki unsurlara eşzamanlı yayını.

---

## 2. Kestirim Elipsi ve Kerteriz Hattı (LOB) Hiyerarşi Mimarisi

Sinyal İstihbaratında yön bulma (DF) ve konum kestirimi hiyerarşisi şu kesin kurallara tabidir:

```
                  ┌─────────────────────────────────────────┐
                  │       KESTİRİM ELİPSİ (EMITTERFIX)      │
                  │  Enlem, Boylam, Yarı-Büyük, Yarı-Küçük  │
                  └────────────────────┬────────────────────┘
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
┌───────────────────────────┐                         ┌───────────────────────────┐
│       ÇOCUKLU ELİPS       │                         │       ÇOCUKSUZ ELİPS      │
│     (Childed Ellipse)     │                         │    (Childless Ellipse)    │
│  En az 2 LOB ile bağlıdır │                         │    Bağlı yerel LOB yoktur │
└───────────┬───────────────┘                         └───────────────────────────┘
            │
            ├──────────────────────────┐
            ▼                          ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│         BAĞLI LOB         │ │         BAĞLI LOB         │
│     (Associated LOB)      │ │     (Associated LOB)      │
│  kestirim_id = Elips_ID   │ │  kestirim_id = Elips_ID   │
└───────────────────────────┘ └───────────────────────────┘

                    ┌───────────────────────────┐
                    │       BAŞIBOŞ LOB         │
                    │   (Unassociated Stray)    │
                    │    kestirim_id = NULL     │
                    │  Tek istasyon yakaladı    │
                    └───────────────────────────┘
```

### A. Varlık Tanımları
1. **LOB (Line of Bearing - Kerteriz Çizgisi / Yön Hattı):**
   * Yön Bulma (DF) algılayıcısının tespit ettiği hedefin geldiği azimut yön açısıdır (0–360° Gerçek Kuzey).
   * Algılayıcı konumu, kerteriz açısı ve yön standart sapmasından oluşur.
2. **Kestirim Elipsi (EmitterFix / CEP Hata Elipsi):**
   * Yayının coğrafi konumunu ve olasılık belirsizlik alanını temsil eden elipstir.
   * Merkez koordinatı, yarı-büyük eksen ($a$, metre), yarı-küçük eksen ($b$, metre) ve yönelim açısından (derece) oluşur.

### B. Hiyerarşik Sınıflandırma ve Doktrinel Anlamı

| Varlık Tipi | Alt Kategori | Tanım Kriteri | Doktrinel Anlamı ve Yorumu |
| :--- | :--- | :--- | :--- |
| **Kestirim Elipsi** | **Çocuklu Elips (Childed)** | Veritabanında `kestirim_id = parent_id` şartını sağlayan $\ge 2$ LOB kaydı bulunan kestirim. | Sahadaki 2 veya daha fazla yerel ED unsurunun açı kesişimiyle (**Triangulation**) doğrulanmış, yüksek güvenilirlikli yerel kestirim. |
| **Kestirim Elipsi** | **Çocuksuz Elips (Childless)** | Veritabanında kendisine bağlı yerel LOB kaydı bulunmayan kestirim. | **ASLA doğrudan Link-16 olarak adlandırılamaz!** Harici NATO Taktik Veri Ağı (Link-16 / Link-22), AWACS intikali, İHA EO/IR izi, zaman farkı (**TDOA**) algoritması veya manuel operatör istihbarat girdisi olabilir. |
| **Ham LOB** | **Bağlı LOB (Associated)** | `kestirim_id` alanı dolu olan kerteriz çizgisi. | Bir kestirim elipsinin hesaplanmasına dahil edilmiş, hedefi teyit eden yöndeş kerteriz hattı. |
| **Ham LOB** | **Başıboş LOB (Unassociated / Stray)** | `kestirim_id` alanı `null` olan kerteriz çizgisi. | Yalnızca tek bir ED unsuru tarafından duyulmuş, diğer istasyonlarca teyit edilememiş sinyal. **Kısa süreli fısıltı yayını, arazi gölgelemesi (maskeleme), kör nokta veya düşmanın frekans atlama hamlesidir.** |

---

## 3. Konum Kestirimi Matematiksel Füzyon Yöntemleri

1. **AOA (Angle of Arrival - Azimut Kesişimi):**
   * İki veya daha fazla algılayıcının azimut açılarının kesiştirilmesidir:
     $$\theta_1, \theta_2 \quad \text{algılayıcılar:} \quad (x_1, y_1), (x_2, y_2)$$
   * Ağırlıklı En Küçük Kareler (Weighted Least Squares - WLS) veya Stansfield algoritması ile kovaryans matrisi çözülür. Yarı-büyük eksen ($a$) ve yarı-küçük eksen ($b$) CEP elipsini oluşturur.
2. **TDOA (Time Difference of Arrival - Varış Zamanı Farkı):**
   * Atomik saatlerle senkronize en az 3 istasyonun sinyali alış zaman farklarını kullanır. Açısal ışın (LOB) üretmez; hiperbollerin kesişimi ile doğrudan 2B koordinat kestirilir. *(Bu nedenle TDOA elipslerinin yerel LOB tablosunda çocuğu bulunmaz).*
3. **FDOA (Frequency Difference of Arrival - Varış Frekansı Farkı):**
   * Hareketli platformların (uçak, İHA, uydu) Doppler frekans kaymasını kullanarak hız ve konum kestirir.

---

## 4. Spektrum Doluluk & Taktik Frekans Kapsama Doktrini

Elektronik Harp'te radar tehditlerinin frekans spektrumundaki kullanım yoğunluğu analiz edilirken iki temel metrik birbirinden kesin olarak ayrılmalıdır:

### A. Frekans Bandı Kapsama Oranı (Interval Union %)
* **Tanım:** Tanımlı bir frekans kanalı (örneğin 50 MHz genişliğindeki `4500 - 4550 MHz` aralığı) içinde düşman veya dost radar yayınlarının kapladığı net frekans genişliğidir:
  $$\text{Doluluk Oranı (\%)} = \frac{\text{Aralıkların Geometrik Birleşimi (Net Kaplanan MHz)}}{\text{Kanal Bant Genişliği (50 MHz)}} \times 100$$
* **Önemli Doktrinel Kural:**
  Eğer bir radar (örneğin S-400 92N6E) `9200 - 9280 MHz` arasında yayın yapıyorsa;
  * **Son 1 saatte** de bu kanalın 50 MHz'lik kısmına basmıştır (**%100**).
  * **Son 24 saatte** de aynı frekans bandına basmaya devam etmiştir (**%100**).
  * Radar frekansını daraltmadığı veya başka bir banda kaymadığı sürece, **frekans bazlı doluluk oranı zaman aralığından bağımsız olarak %100 kalabilir. Bu bir hata değil, radarın aynı frekansta yayın yapmaya devam ettiğinin kanıtıdır.**

### B. Zaman-Frekans Temas Trafiği (Activity / Pulse Density)
* **Tanım:** İlgili zaman diliminde o kanalda tespit edilen toplam darbe veya yayın adedidir.
* 1 saat ile 24 saat arasındaki gerçek fark burada ortaya çıkar:
  * Son 1 Saat: 15.464 tespit
  * Son 24 Saat: 78.025 tespit (5 kat trafik artışı)
* Raporlamada mutlaka hem frekans doluluk oranı (`%`) hem de tespit sayısı (`temas`) birlikte verilmelidir.

---

## 5. Teşhis Kimliği (Identity) Standart Renk Kodları

Askeri Komuta Kontrol (C2) arayüzlerinde NATO APP-6 standartlarına uyumlu ve karanlık temada yüksek kontrastlı renk kodlaması uygulanır:

| Teşhis Kimliği | Standart Anlamı | Hex Kodu | Degrade Tonları | Görsel Karakteristik |
| :--- | :--- | :---: | :---: | :---: |
| **DÜŞMAN (Hostile)** | Teyitli Düşman / Tehdit | `#ef4444` | `#f87171` $\rightarrow$ `#dc2626` | Parlak Kırmızı |
| **MUHTEMEL DÜŞMAN (Suspect)** | Şüpheli / Düşman Olabilir | `#f97316` | `#fb923c` $\rightarrow$ `#ea580c` | Taktiksel Turuncu |
| **BİLİNMEYEN (Unknown)** | Teşhis Edilememiş Emisyon | `#eab308` | `#fde047` $\rightarrow$ `#ca8a04` | Taktiksel Sarı |
| **DOST (Friend)** | Teyitli Dost Unsur / Müttefik | `#10b981` | `#34d399` $\rightarrow$ `#059669` | Zümrüt Yeşili |
| **TARAFSIZ (Neutral)** | Sivil / Ticari / Tarafsız | `#a855f7` | `#c084fc` $\rightarrow$ `#9333ea` | **Canlı Mor / Menekşe** |

---

## 6. Yapay Zeka Taktiksel Raporlama Kuralları

Copilot veya Ajan taktik brifing üretirken şu 4 altın kurala uymalıdır:

1. **Peşin Hükümlü Etiketleme Yapma:** Çocuksuz elipsleri operatör notunda veya hedef kaynağında açıkça yazmadıkça "Link-16" olarak adlandırma; "Harici Taktik Link / Doğrudan Kestirim" olarak belirt.
2. **Oranları Mutlaka Belirt:** Kerteriz hattı raporlarken Başıboş LOB oranını (`basibos_lob / total_lob * 100%`) daima belirt. %50'nin üzerindeki başıboşluk oranı düşmanın gizlenme veya arazi maskelemesi yaptığına işaret eder.
3. **Zaman-Kapsam Ayrımını Vurgula:** Taktiksel anlık hareket ("Son 1 saat") ile stratejik yaşam döngüsü ("Son 24 saat") analizini net olarak ayır; frekans kapsama yüzdesinin yanında mutlaka tespit adedini raporla.
4. **Tehdit Önceliği Sıralaması:** Atış Kontrol radarları (S-400, Pantsir, Patriot) ve Kriptolu/Darbe-Patlama telsizleri her zaman brifinglerin en üstünde kırmızı uyarıyla yer almalıdır.
