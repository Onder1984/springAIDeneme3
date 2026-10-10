---
name: sigint-c2-doctrine
description: >-
  Authoritative knowledge base and doctrinal rules for military Electronic Warfare (EW / EH)
  and Signal Intelligence (SIGINT) systems covering ELINT (Radar emissions, PRI, PW, MOP, ATP,
  HSS, ET/Jamming, Harassment) and COMINT (Radio networks, HF/VHF, crypto, burst, dialogue traffic).
  Defines precise rules for Ellipse vs. LOB hierarchy (Childed vs. Childless, Connected vs.
  Stray/Unassociated LOBs) and NATO Tactical Data Links (Link-16, Link-22, JREAP).
---

# SIGINT & Electronic Warfare (C2 EW) Doctrine Guide

This skill provides comprehensive doctrinal standards, definitions, mathematical foundations, and analysis rules for military **Signals Intelligence (SIGINT)** and **Electronic Warfare (EW / EH)** decision support systems.

---

## 1. Core SIGINT Taxonomy

```
                       ┌─────────────────────────────────────┐
                       │     SİNYAL İSTİHBARATI (SIGINT)     │
                       └──────────────────┬──────────────────┘
                                          │
                 ┌────────────────────────┴────────────────────────┐
                 ▼                                                 ▼
    ┌───────────────────────────┐                     ┌───────────────────────────┐
    │       ELINT (RADAR)       │                     │     COMINT (MUHABERE)     │
    │ Non-communication Emitters│                     │ Communication Transmissions│
    └───────────────────────────┘                     └───────────────────────────┘
```

### A. ELINT (Electronic Intelligence - Radar & Non-Comm)
* **Emitter Types:** Early Warning (Erken İhbar), Target Acquisition (Arama/Takip), Fire Control (Atış Kontrol), Missile Guidance (Füze Güdüm), Airborne Intercept (AI / Uçak Radarı), Naval Navigation (Gemi Seyir).
* **Signal Parameters:**
  * **RF (Radio Frequency):** Carrier frequency in MHz/GHz.
  * **PRI (Pulse Repetition Interval):** Time between consecutive pulses in microseconds (µs). Staggered, jittered, or fixed.
  * **PW (Pulse Width):** Duration of a single pulse in microseconds (µs).
  * **MOP (Modulation on Pulse):** Linear FM (Chirp), Barker code, Polyphase (P1-P4), Frequency Hopping, Fixed.
  * **ATP (Antenna Scan Period):** Time taken for one complete antenna rotation in seconds.
* **Electronic Attack & Harassment (ET & Taciz):**
  * **ET (Electronic Attack / Jamming):** Active noise jamming (gürültü), deception jamming (aldatma), repeater jamming.
  * **Taciz (Radar Lock / Illumination):** A fire-control radar locking onto friendly assets without launching a missile.

### B. COMINT (Communications Intelligence - Radio & Telecom)
* **Bands:**
  * **HF (High Frequency, 3–30 MHz):** Ionospheric reflection (skywave), beyond-line-of-sight strategic communications.
  * **VHF (Very High Frequency, 30–300 MHz):** Tactical line-of-sight, ground troops, air-to-ground, marine distress.
  * **UHF (Ultra High Frequency, 300–3000 MHz):** Tactical data links, SATCOM, UAV telemetry.
* **Transmission Modes:**
  * **Voice (Ses):** Analog FM/AM or digital voice.
  * **Crypto (Kriptolu):** Encrypted voice or data stream.
  * **Burst / Frequency Hopping (Darbe-Patlama):** High-speed, short-duration data burst designed to avoid detection (LPI/LPD).
* **Traffic Patterns:**
  * **1-to-1 (Point-to-Point):** Dedicated command/tactical link between two call signs.
  * **Broadcast (Net):** One-to-many communication across an entire tactical network.

---

## 2. Ellipse & LOB Hierarchical Architecture

### A. Core Definitions
1. **LOB (Line of Bearing / Kerteriz Çizgisi):**
   * A single directional ray measured by an azimuth Direction Finding (DF) sensor pointing towards the suspected emitter.
   * Attributes: Origin latitude/longitude, bearing angle (0–360° True North), azimuth variance.
2. **Kestirim Elipsi (EmitterFix / CEP Uncertainty Ellipse):**
   * A geographical uncertainty zone representing the estimated emitter position.
   * Attributes: Center latitude/longitude, semi-major axis (meters), semi-minor axis (meters), ellipse orientation angle (degrees).

### B. Hierarchy Classification Rules

| Entity | Category | Definition | Doctrinal Meaning |
| :--- | :--- | :--- | :--- |
| **Kestirim Elipsi** | **Çocuklu Elips (Childed)** | Veritabanında ona bağlı (`kestirim_id = parent_id`) en az 2 LOB kaydı bulunan kestirim. | Sahadaki 2+ yerel algılayıcının AOA kerteriz kesişimiyle (triangulation) doğrulanmış yüksek güvenilirlikli yerel hedef. |
| **Kestirim Elipsi** | **Çocuksuz Elips (Childless)** | Veritabanında bağlı yerel LOB kaydı bulunmayan doğrudan kestirim. | **ASLA doğrudan Link-16 olarak etiketlenemez!** Link-16, Link-22, AWACS intikali, İHA taktik izi, doğrudan TDOA algoritması veya manuel operatör girdisi olabilir. |
| **Ham LOB** | **Bağlı LOB (Associated)** | `kestirim_id` alanı dolu olan kerteriz çizgisi. | Bir kestirim elipsine dahil olmuş, o elipsin hesaplanmasına katılmış yön hattı. |
| **Ham LOB** | **Başıboş LOB (Unassociated / Stray)** | `kestirim_id` alanı `null` olan kerteriz çizgisi. | Tek istasyonca yakalanmış, diğer istasyonlarca teyit edilememiş hat. Kısa süreli fısıltı yayını, arazi engellemesi, kör nokta veya erken ihbar sinyalidir. |

---

## 3. Direction Finding Mathematics & Fusion Methods

1. **AOA (Angle of Arrival / Azimuth Intersection):**
   * Intersects two or more bearings:
     $$\theta_1, \theta_2$$ from sensors at $$(x_1, y_1), (x_2, y_2)$$.
   * Solved using Weighted Least Squares (WLS) or Stansfield algorithm to generate the covariance matrix determining the semi-major ($a$) and semi-minor ($b$) axes of the error ellipse.
2. **TDOA (Time Difference of Arrival):**
   * Uses synchronized hyperbolic time difference measurements between 3+ stations. Does not produce a single bearing ray; directly calculates hyperbolic position intersection. (Explains why TDOA ellipses are childless in AOA LOB databases).
3. **FDOA (Frequency Difference of Arrival):**
   * Uses Doppler frequency shift for moving platforms (aircraft, satellites).

---

## 4. Tactical Reporting & Analysis Rules for AI

When generating briefings or answering tactical questions:
1. **No Peşin Etiketleme:** Do not label childless ellipses as "Link-16" unless the `operator_notu` or `hedef_kaynagi` explicitly says so.
2. **Always State Ratios:** When reporting on LOBs, always provide the Başıboş LOB oranı (`basibos_lob / total_lob * 100%`). High stray rates (>50%) signal terrain masking or agile emitters.
3. **Time-Bounded Precision:** Always distinguish between short-term tactical movement ("Son 1 saat") and strategic pattern of life ("Son 24 saat").
4. **Threat Prioritization:** Atış Kontrol radarları (Pantsir, S-400, Patriot) ve Kriptolu/Darbe-Patlama telsiz ağları her zaman brifinglerin ilk sırasında yer almalıdır.
