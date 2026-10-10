# Taktik C2 Elektronik Harp (EW) & Büyük Veri Kuralları

Bu dosya, `AntigravitySpringAIDeneme2` projesinin temel mimari standartlarını, büyük veri koruma kurallarını ve geliştirme yönergelerini belirler.

---

## 📌 GRUP A: Mimari, Altyapı ve Veri Kalıcılığı

1. **Kalıcı PostgreSQL & Docker Volume:**
   - Docker konteyneri kapansa bile `tactical_ew_postgres_data` volume'ü ile veriler korunur.
   - Hibernate `ddl-auto: update` modunda çalışır; tablolar asla silinmez/sıfırlanmaz.

2. **Akıllı Veri Başlatıcı (`DataInitializer`):**
   - Sistem açılışında veritabanını sayar; kayıtlar mevcutsa veriyi ezmez, mükerrer veri üretmez (`VERİ KALICILIĞI AKTİF`).

3. **JVM Dil Standartı (`Locale.ENGLISH`):**
   - Google GenAI API'nin JSON şemalarındaki `INTEGER`/`STRING` tiplerinin Türkçe karakter dönüşümünden (`i -> İ`) dolayı 400 hatası vermesini engellemek için sistem dili ASCII/İngilizce'ye sabitlenir.

---

## 📌 GRUP B: Büyük Veri & LLM Koruma Kalkanı (Defensive Tooling)

4. **Makro Sorularda Doğrudan SQL Agregasyonu:**
   - *"Genel durum nedir?"*, *"Bant dağılımı ver"* gibi sorularda tüm veri belleğe çekilmez; veritabanında `COUNT` ve `GROUP BY` çalıştıran makro istatistik araçları (`getTacticalMacroStats`, `getComintMacroStats`) kullanılır.

5. **Öncelikli Tehdit Tavanı (Top-K Ceiling):**
   - *"Tehditleri listele"* dendiğinde modelin context penceresini şişirmemek için en kritik ilk 15 hedef (`queryPriorityThreats`, `queryPriorityComintThreats`) çekilir.

6. **Dinamik Parametre Temizleme (`cleanParam`):**
   - Modelin gönderdiği `"ALL"`, `"TÜMÜ"`, `"*"` gibi serbest metinler SQL için `null`a dönüştürülür, böylece sorguların boş dönmesi engellenir.

7. **Çift Yönlü Sayfalama Motoru (Dual Pagination):**
   - Kullanıcı butonla (`[Sonraki Sayfa ➔]`) veya doğal dille (*"devamını ver"*, *"sonraki sayfayı getir"*) gezinme yapabilir.
   - Önceki filtreler oturum hafızasında (`SESSION_STATES` / `ConcurrentHashMap`) güvenle saklanır, operatör filtreleri tekrar yazmak zorunda kalmaz.

---

## 📌 GRUP C: Veri Ayrımı ve Askeri Analiz Kuralları

8. **Kesin Radar ve Muhabere Ayrımı:**
   - Veritabanında Radar ve COMINT kayıtları vardır.
   - Yapılan sorgularda radar ya da muhabere açıkça belirtilmez ise ikisi için de sorgu atılabilir (müşterek sorgulama).
   - Ancak radar ya da muhabere diye açıkça belirtilirse sorgulama yalnızca o alana uygun araçlar ile yapılır.

9. **Askeri Determinizm:**
   - Model asla kafasından koordinat, frekans, radar adı veya telsiz çağrı adı uyduramaz; her iddia bir tool çağrısına dayanmak zorundadır.

10. **Aralık Kesişimi (Interval Overlap):**
    - Frekans ve diğer sayısal sorgularda tekil eşitlik yerine min-max aralık kesişim mantığı (`minA <= maxB && maxA >= minB`) uygulanır.

---

## 📌 GRUP D: Çıktı Formatı ve Kullanıcı Arayüzü Standartları

12. **Zengin Markdown Tabloları:**
    - Tüm listeler ve parametreler okunaklı Markdown Tabloları (`| ... |`) ve hiyerarşik askeri başlıklar halinde sunulur. Grafik ve harita çizimleri kesinlikle yoktur.

13. **Tıklanabilir Parametre Köprüleri:**
    - Yanıtlarda geçen hedef ID'leri (`[EMI-xxxx]`, `[COM-xxxx]`) formatında sunularak arayüzde 37 ve 30 parametreli detay pencerelerini tetikler.

14. **SIGINT/EW Doktrini: Ham Kerteriz (HAM_LOB) ve Kestirim (KESTIRIM_ELIPS) Ayrımı:**
    - **HAM_LOB (Line of Bearing / Kerteriz):** Tekil algılayıcı/DF istasyonunun yön ölçümüdür. Hedef yayın konumu (enlem, boylam, elips eksenleri ve oryantasyon) KESİNLİKLE NULL'dur. Yalnızca algılayıcı konumu ve kerteriz açısı bulunur.
    - **KESTIRIM_ELIPS (Cross-Fix / Kestirim Elipsi):** Birden fazla LOB'un kesiştirilmesiyle hesaplanmış hedef konum elipsidir (enlem, boylam, yarı-büyük/küçük eksen ve oryantasyon doludur).
    - **LOB-Kestirim İlişkisi:** Kestirime katkı sunan LOB'lar `kestirim_id` üzerinden o kestirimin ID'sini tutar. Henüz kestirime girmemiş başıboş LOB'larda `kestirim_id` boştur (NULL). Kestirim detay penceresi ilgili tüm katkı veren LOB'ları listeler ve çift yönlü navigasyon sağlar.

