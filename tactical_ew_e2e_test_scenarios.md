# 🛡️ Taktik EH Komuta Kontrol Sistemi - Otonom E2E Tarayıcı Test Senaryoları (Test Suite)

Bu doküman, Antigravity AI Otonom Tarayıcı Ajanı (`browser_subagent`) tarafından icra edilecek uçtan uca (E2E) test senaryolarını, doğrulama kriterlerini ve beklenen çıktıları tanımlar.

**Hedef Uygulama:** `http://localhost:8082`  
**Test Koşucu:** Antigravity Autonomous Browser Subagent  
**Raporlama Çıktısı:** Ekran görüntüleri, WebP video kaydı ve Adım Adım PASS/FAIL Matrisi  

---

## 📋 Test Senaryoları Özeti

| No | Senaryo Adı | Odak / Kapsam | Kritiklik |
|:---|:---|:---|:---:|
| **TC-01** | Sistem Genel Sağlığı ve Üst Bar (HeaderBar) Metrikleri | Sayfa yüklenişi, makro istatistikler (160 Radar, 120 COMINT), Online durumu | **Yüksek** |
| **TC-02** | Radar (ELINT) Tablosu, Sayfalama & Kolon Sıralama | 15'erli sayfalama, Frekans (ASC/DESC) ve Genlik sıralaması | **Kritik (Madde 6 & 7)** |
| **TC-03** | Bölge/Yer Bilgisi ve Dinamik Kolon Yönetimi | Yeni "Bölge" kolonu, Sütunları Yönet Popover, kolon gizleme/gösterme | **Kritik (Madde 2)** |
| **TC-04** | Açılır/Kapanır Gelişmiş Filtreler Paneli | Min-Max Frekans (9000-10000 MHz) filtreleme, aralık doğrulaması | **Kritik (Madde 5)** |
| **TC-05** | Muhabere (COMINT) Tablosu & Kripto Filtrelemesi | 120 COMINT kaydı, Kripto filtresi seçimi (38 kayıt), 500 hatası olmaması | **Kritik (Madde 8)** |
| **TC-06** | Taktik Copilot (Chatbot), Zaman Formatı & Tablo Yönlendirmesi | Taktik soru sorma, `GG.AA.YYYY SS:dd:ss` zaman formatı, tablo etkileşimi | **Kritik (Madde 1)** |
| **TC-07** | 37/30 Parametreli Hedef Detay Paneli (Drawer) | Satır tıklaması ile yan panelin açılması, teknik parametrelerin gösterimi | **Yüksek** |

---

## 🔍 Detaylı Test Senaryoları ve Adımları

### TC-01: Sistem Genel Sağlığı ve Üst Bar (HeaderBar) Metrikleri
- **Amaç:** Arayüzün hatasız açıldığını ve backend REST servislerinden genel taktik istatistiklerin çekildiğini doğrulamak.
- **Test Adımları:**
  1. `http://localhost:8082` adresine git.
  2. Başlık çubuğundaki (HeaderBar) sistem durum etiketini kontrol et. ("SİSTEM ÇEVRİMİÇİ" veya yeşil gösterge).
  3. Makro metrik kartlarını oku: Toplam Radar Emisyonu (160) ve Toplam Muhabere Emisyonu (120) sayılarının render edildiğini doğrula.
- **Beklenen Sonuç:** Sayfa hatasız yüklenir, metrikler boş (NaN veya 0) gelmez, 160 Radar ve 120 Muhabere sayısı görünür.

---

### TC-02: Radar Tablosu Sayfalama & Kolon Sıralama (Madde 6 & 7 Doğrulaması)
- **Amaç:** Radar tablosundaki verilerin 15'erli sayfalandığını ve tüm kolonlarda çift yönlü sıralamanın (ASC/DESC) çalıştığını doğrulamak.
- **Test Adımları:**
  1. **"📡 Radar (ELINT) Tablosu"** sekmesine tıkla.
  2. Tablo altında sayfalama kontrolünü doğrula (Toplam 160 kayıt, Sayfa 1/11).
  3. Sayfalama butonundan **2. Sayfaya** tıkla; satır kimliklerinin (EMI-xxxx) değiştiğini doğrula. Tekrar 1. Sayfaya dön.
  4. **Frekans (MHz)** kolon başlığına tıkla (Artan Sıralama - ASC).
     - İlk satırdaki radarın düşük frekanslı (152 MHz - Nebo-M) olduğunu doğrula.
  5. **Frekans (MHz)** kolon başlığına tekrar tıkla (Azalan Sıralama - DESC).
     - İlk satırdaki radarın yüksek frekanslı (10998 MHz civarı) olduğunu doğrula.
  6. **Genlik (dBm)** kolon başlığına tıkla; sıralamanın sunucu tarafında hatasız çalıştığını doğrula.
- **Beklenen Sonuç:** Sayfalama akıcıdır, kolon sıralamaları tetiklendiğinde API'ye `sortField` ve `sortOrder` iletilir ve tablo doğru sıralanır.

---

### TC-03: Bölge/Yer Bilgisi ve Dinamik Kolon Yönetimi (Madde 2 Doğrulaması)
- **Amaç:** Yeni eklenen "Bölge / Yer Bilgisi" kolonunu ve kullanıcının kolonları açıp kapatabilmesini sağlayan "Sütunları Yönet" özelliğini doğrulamak.
- **Test Adımları:**
  1. Radar tablosunda **"Bölge / Yer Bilgisi"** kolonunun varlığını ve hücrelerdeki metinleri ("Ege Denizi / Midilli Doğusu", "Akdeniz / Girit Açıkları" vb.) kontrol et.
  2. Tablonun sağ üstündeki **"Sütunları Yönet"** butonuna tıkla.
  3. Açılan Popover menüde "Bölge / Yer Bilgisi" veya "PRI (µs)" onay kutusunun (checkbox) işaretini kaldır.
  4. Tabloda ilgili kolonun anında gizlendiğini doğrula.
  5. Popover menüden işareti tekrar seç ve kolonun tabloya geri geldiğini doğrula.
- **Beklenen Sonuç:** Kolon yönetimi dinamik olarak çalışır, tablo kullanıcı tercihine anında uyum sağlar.

---

### TC-04: Açılır Gelişmiş Filtreler Paneli (Madde 5 Doğrulaması)
- **Amaç:** Katlanabilir gelişmiş filtreleme panelinin min/max frekans, PRI, PW ve bölge kriterlerini filtreleyebildiğini doğrulamak.
- **Test Adımları:**
  1. **"Gelişmiş Filtreler"** panel başlığına tıklayarak paneli aç.
  2. **Min Frekans (MHz)** kutusuna `9000`, **Max Frekans (MHz)** kutusuna `10000` değerini gir.
  3. **"Filtrele"** butonuna bas.
  4. Tablonun filtrelenmiş verileri getirdiğini doğrula (Kayıt sayısı 160'tan 8'e düşmeli).
  5. Tabloda listelenen radarların frekanslarının 9000 ile 10000 MHz aralığında olduğunu gözlemle.
  6. **"Temizle"** butonuna bas; filtrenin sıfırlandığını ve tablonun tekrar 160 kayda döndüğünü doğrula.
- **Beklenen Sonuç:** Gelişmiş filtreler JPA backend kriterlerine doğru yansır, tablo filtrelenir ve sıfırlanabilir.

---

### TC-05: Muhabere (COMINT) Tablosu & Kripto Filtrelemesi (Madde 8 Doğrulaması)
- **Amaç:** Düzeltilen COMINT sorgusunun 500 hatası almadan kriptolu haberleşmeleri filtreleyebildiğini doğrulamak.
- **Test Adımları:**
  1. **"📻 Muhabere (COMINT) Tablosu"** sekmesine tıkla.
  2. Tablonun ilk 15 kaydının ve toplam 120 kaydın geldiğini doğrula.
  3. Filtre alanındaki **"Haberleşme"** veya **"Kriptolu"** seçeneğini seç.
  4. Tablonun yenilenmesini bekle (Konsolda veya ekranda 500 Internal Server Error OLMADIĞINI doğrula).
  5. Toplam 38 kayıt geldiğini ve satırlardaki "Haberleşme Şekli" etiketlerinin "KRIPTO" olduğunu doğrula.
  6. Tarih formatının `GG.AA.YYYY SS:dd:ss` formatında olduğunu kontrol et.
- **Beklenen Sonuç:** Kripto filtresi hatasız çalışır, 38 adet kriptolu muhabere kaydı listelenir.

---

### TC-06: Taktik Copilot (Chatbot), Zaman Formatı & Yönlendirme (Madde 1 Doğrulaması)
- **Amaç:** AI Taktik Copilot'un taktik sorguyu işlediğini, tarihleri yerel formatta sunduğunu ve tablo yönlendirmesi sağladığını doğrulamak.
- **Test Adımları:**
  1. **"💬 Taktik Copilot"** sekmesine dön.
  2. Sohbet giriş alanına şu soruyu yaz:
     `"Ege Denizi bölgesindeki tespit edilen düşman radarlarını listele"`
  3. **Gönder** butonuna bas ve AI cevabını bekle.
  4. Asistanın yanıtındaki Markdown tablosunu kontrol et:
     - Zaman/Tespit sütunundaki formatın `GG.AA.YYYY SS:dd:ss` (Örn: `30.09.2026 14:...`) olduğunu doğrula.
  5. Asistan yanıtındaki hedef kimliğine veya tablo yönlendirme butonuna tıkla; ilgili detay veya sekmenin açıldığını teyit et.
- **Beklenen Sonuç:** LLM doğru araçları çağırır, standart tarih formatında yanıt verir ve kullanıcıyı ilgili veriye bağlar.

---

### TC-07: 37/30 Parametreli Hedef Detay Paneli (Drawer)
- **Amaç:** Tablolardaki bir kaydın üzerine tıklandığında kapsamlı teknik detay panelinin açıldığını doğrulamak.
- **Test Adımları:**
  1. Radar tablosundaki herhangi bir satırın **"Detay"** butonuna veya satırına tıkla.
  2. Sağ taraftan **"Radar Emisyon Detayı"** panelinin (Drawer) açıldığını doğrula.
  3. Teknik parametrelerin (Frekans listesi, PRI listesi, PW listesi, Polarizasyon, Karıştırma/ET durumu, Taktik Konum) görüntülendiğini kontrol et.
  4. Drawer kapatma butonuna (X) basarak paneli kapat.
- **Beklenen Sonuç:** Yan panel akıcı şekilde açılır, veriler REST API'den (`/emissions/{id}`) çekilerek gösterilir ve kapatılabilir.

---

## 🚀 Çalıştırma Yöntemi

Kullanıcı sohbet penceresinden şu komutlardan birini verdiğinde Antigravity bu senaryoları sırasıyla icra eder:
- *"Testleri koş"*
- *"Tarayıcı üzerinden testleri çalıştır"*
- *"Sistemi doğrula"*

Test tamamlandığında Antigravity:
1. Her senaryonun yanına `[PASS]` veya `[FAIL]` durumunu yazar.
2. Yakaladığı ekran görüntülerini ve video kaydını kullanıcıya sunar.
3. Varsa bulunan sapmaları veya hataları listeler.
