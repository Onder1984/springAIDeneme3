# 📋 Taktik EH Komuta Kontrol Sistemi - Geliştirme ve İyileştirme TODO Listesi

Bu doküman, sistemde daha sonra ele alınmak üzere beklemeye alınan teknik tespitleri, kök neden analizlerini ve planlanan çözüm adımlarını içerir.

---

## 📌 TODO-01 (Madde 4): Sinyal Genliği (dBm) Sorguları ve LLM Eşleme İyileştirmesi

### 1. Belirtilen Sorun
Operatör taktik asistana *"genliği -80 dBm'den küçük kaç tane radar var"* sorusunu yönelttiğinde, veritabanında gerçekte **11 adet** kayıt olmasına rağmen asistan *"5 adet tespit edilmiştir"* şeklinde eksik yanıt vermektedir.

### 2. Kök Neden Analizi
- **LLM Parametre Eşleyememe Sorunu:** `RadarEmissionTools.queryEmissions` aracında `minGenlikDbm` ve `maxGenlikDbm` parametreleri tanımlıdır. Ancak LLM, negatif desibel (dBm) mantığında "-80 dBm'den küçük/zayıf" ifadesinin matematikte `maxGenlikDbm = -80.0` anlamına geldiğini çıkaramamıştır.
- **Numune Üzerinden Sayma:** LLM bu parametreyi araca geçiremeyince `queryEmissions(limit=10)` çağrısı yaparak filtre koymadan ilk 10 kaydı çekmiş; gelen bu 10 kayıttan tesadüfen 5 tanesinin genliği -80'den küçük olduğu için veritabanının tamamı yerine elindeki 10 kaydı sayarak "5 adet" demiştir.

### 3. Planlanan Çözüm Adımları
1. **Açıklayıcı ToolParam Etiketleri:**
   `RadarEmissionTools` ve `ComintEmissionTools` içindeki genlik parametrelerine açık `@ToolParam` açıklamaları eklenecek:
   - `minGenlikDbm`: *"Sinyal gücü alt sınırı (dBm). Örneğin '-50 dBm'den güçlü/büyük sinyaller' istendiğinde minGenlikDbm = -50.0 verilmelidir."*
   - `maxGenlikDbm`: *"Sinyal gücü üst sınırı (dBm). Örneğin '-80 dBm'den küçük/zayıf sinyaller' istendiğinde maxGenlikDbm = -80.0 verilmelidir."*
2. **Sistem Prompt (Direktif) Kuralı:**
   `TacticalAgentService` sistem yönergesine negatif dBm filtreleme kuralı eklenerek LLM'in genel sayaç sorgularında `queryEmissions` genlik parametrelerini doğru doldurması garanti altına alınacak.
3. **Makro İstatistik Genişletmesi:**
   Gerektiğinde `getTacticalMacroStats` içerisine genlik seviye dağılımı (Zayıf: <-80 dBm, Orta: -80..-60 dBm, Güçlü: >-60 dBm) eklenecek.

---

## ✅ TAMAMLANDI - Madde 6: Tablolarda Frekans Sıralama Mantığı ve Düzeltmesi

### 1. Durum: **ÇÖZÜLDÜ VE DOĞRULANDI**
Operatörün *"Tabloda frekansa göre sıralama neye göre yapılıyor?"* sorusu yanıtlanmış ve sıralama altyapısı eksiksiz çalışır hale getirilmiştir.

### 2. Yapılan İşlemler ve Çözüm:
1. **Sıralama Kriteri Netleştirildi:** Radarlar frekans aralığına sahip olduğundan, sıralamanın yayının başlangıç frekansı olan **`minFrekansMhz`** değerine göre yapıldığı tespit edildi ve doğrulandı.
2. **Kolon dataIndex Eşleştirmesi:** Arayüzdeki (React/AntD) Frekans kolonuna `dataIndex: 'minFrekansMhz'` ve `sorter: true` tanımlandı.
3. **Backend Dinamik JPA Sort:** `RadarEmissionTools.getEmissionsPage` metodunda `minFrekansMhz` alanı JPA `Sort` kriterlerine dinamik olarak bağlandı.
4. **E2E Doğrulaması (TC-02):** Canlı tarayıcı testinde (TC-02) Artan (ASC: 152.0 MHz - Nebo-M) ve Azalan (DESC: 10998.0 MHz - AN/APG-81) sıralamanın hatasız çalıştığı doğrulanmıştır.

---

## 📌 TODO-03 (Madde 9): Radar VHF/UHF ve Frekans Bant Tipine Göre Adet/Filtre Sorguları

### 1. Belirtilen Sorun
Asistana *"VHF radarlarının adedi"* veya *"X-band radar sayısı"* sorulduğunda hatalı yanıt vermektedir. Toolda bu parametrenin eksik olduğu gözlemlenmiştir.

### 2. Kök Neden Analizi
- **Veri Modeli Farkı:** Muhabere (COMINT) tablosunda `tip` kolonu enum olarak (`HF`, `VHF`) doğrudan mevcuttur. Ancak Radar tablosunda bant tipi bir metin kolonu olarak değil, MHz cinsinden sayısal frekans aralığı (`minFrekansMhz` - `maxFrekansMhz`) olarak tutulmaktadır.
- Bu nedenle LLM radarları sorgularken `tip=VHF` gibi bir parametreyi `queryEmissions` aracında bulamamaktadır.

### 3. Planlanan Çözüm Adımları
1. **Radar Tool'una Bant Parametresi Eklenmesi:**
   `RadarEmissionTools.queryEmissions` metoduna doğrudan `frekansBandi` (`VHF`, `UHF`, `L_BAND`, `S_BAND`, `C_BAND`, `X_BAND`, `KU_BAND`) parametresi eklenecek.
2. **Otomatik Frekans Aralığı Çeviricisi:**
   Tool katmanında girilen bant adı otomatik olarak standart askeri/NATO frekans karşılıklarına dönüştürülecek:
   - `VHF` -> 30 MHz - 300 MHz
   - `UHF` -> 300 MHz - 1000 MHz
   - `L-Band` -> 1000 MHz - 2000 MHz
   - `S-Band` -> 2000 MHz - 4000 MHz
   - `C-Band` -> 4000 MHz - 8000 MHz
   - `X-Band` -> 8000 MHz - 12000 MHz
   - `Ku-Band` -> 12000 MHz - 18000 MHz
3. **Makro İstatistiğe Bant Dağılımı Eklenmesi:**
   `getTacticalMacroStats` çıktısına frekans bantlarına göre radar sayıları eklenerek asistanın tek sorguda tüm bant adetlerini doğru söylemesi sağlanacak.

---

## 📌 TODO-04: LLM Tarafından Uydurulan / Karıştırılan Radar ID (Hallucination) Tespiti ve Önleme Mekanizması

### 1. Belirtilen Sorun
Operatör Copilot'a ardışık filtreleme sorguları yönelttiğinde (örn. *"Dwell-switch PRI kullanan ve frekansı 10 GHz üstü olan radarları listele"*, ardından *"emin misin"*), Copilot yanıtta **`EMI-E48A2C91B87F4A12`** gibi bir ID listelemektedir. Ancak bu ID radar tablosunda aratıldığında bulunamamakta ve doğrudan API çağrısında `500 Internal Server Error (Emisyon bulunamadı)` hatası alınmaktadır.

### 2. Kök Neden Analizi
- **Karmaşık Hex UUID Formatı:** 200.000 kayıt eklendiğinde `MassDataSeederService` tarafından üretilen ID'ler 16 karakterli rasgele hex dizileridir (örn. `EMI-54F460E240034833`).
- **LLM Bağlam Karışması & Halüsinasyon:** LLM (Gemini), ardışık sorgularda onlarca emisyon detayını bağlam penceresine alıp işlerken, gerçekte veritabanında var olan bir ID'nin (örn. `EMI-E48A8D9D5C9B428A`) karakterlerini belleğinde karıştırmış ve desen benzerliğinden ötürü veritabanında fiziksel olarak var olmayan hayali bir ID (`EMI-E48A2C91B87F4A12`) üretmiştir.
- **Hata Yönetimi (Status 500):** Olmayan bir ID arandığında backend'in `404 Not Found` yerine `500 Internal Server Error` fırlatması arayüzde sessiz hataya veya kilitlenmeye neden olmaktadır.

### 3. Planlanan Çözüm Adımları
1. **Agent Sistem Yönergesi (Prompt Guardrail):**
   `TacticalAgentService.java` içindeki `SYSTEM_DIRECTIVE` determinizm maddesine katı bir kural eklenecek:
   > *"ASLA kafandan, tahminden veya benzerlikten `EMI-xxxx` veya `COM-xxxx` kimliği türetme/uydurma! Yalnızca sana tool'ların döndürdüğü JSON nesnelerindeki tam 'id' değerlerini birebir kopyalayarak yanıtta kullan."*
2. **API Hata Yönetimi (`404 Not Found` & Global Exception Handler):**
   `TacticalEmissionEntity` veya `TacticalComintEntity` bulunamadığında `IllegalArgumentException` yerine `ResponseStatusException(HttpStatus.NOT_FOUND)` veya `@ControllerAdvice` ile temiz HTTP 404 dönülmesi; arayüzün kullanıcıya "Kayıt veritabanında bulunamadı" uyarısı göstermesi sağlanacak.
3. **Format Standartlaştırması:**
   Büyük ölçekli veri üretimlerinde LLM'in akılda tutmasını ve token üretimini kolaylaştıracak daha okunabilir/yapısal ID desenleri (örn: `EMI-T-XXXXXX`) değerlendirilecek.

---

## ✅ TAMAMLANDI - Madde 10: SIGINT/EW Doktrini - Ham LOB (Kerteriz) ve Kestirim (Hata Elipsi) Mimarisi

### 1. Durum: **MİMARİ VE UYGULAMA TAMAMLANDI, CANLI VERİDE DOĞRULANDI**
Operatörün belirttiği gerçekçi Elektronik Harp ve SIGINT doktrini sisteme kazandırılmıştır:
- Tüm radar ve muhabere yayınları **`KESTIRIM_ELIPS` (Cross-Fix / Elips)** veya **`HAM_LOB` (Line of Bearing / Kerteriz)** olarak sınıflandırılmıştır.
- **`HAM_LOB` Kayıtları:** Tekil DF algılayıcı yön kestirimidir. Hedef elips koordinatları (`yayin_enlem`, `yayin_boylam`, elips eksenleri ve yönelim) KESİNLİKLE `NULL` yapılmıştır.
- **İlişkili LOB:** Bir kestirimin hesabında kullanılan LOB'lar `kestirim_id` üzerinden o kestirime (`EMI-xxxx` veya `COM-xxxx`) bağlanmıştır.
- **Başıboş (Orphan) LOB:** Henüz herhangi bir kestirim algoritmasına girmemiş, kestirilmeyi bekleyen münferit yön ölçümlerinde `kestirim_id` değeri `NULL` olarak tutulmaktadır.
- **`KESTIRIM_ELIPS` Kayıtları:** Tam hata elipsi koordinatlarına sahiptir ve arka planda kendisine katkı sunan tüm bağlı LOB kimliklerini (`iliskiliLobIdListesi`) barındırır.

### 2. Yapılan İşlemler ve Doğrulama
1. **Veritabanı Şeması & Migrasyon:**
   - PostgreSQL `tactical_ew_db_3` üzerinde `kayit_tipi` ve `kestirim_id` kolonları ile B-Tree indeksleri oluşturuldu.
   - 402.280 taktik kaydın tamamında LOB kayıtlarının elips koordinatları sıfırlandı; demo ve operasyonel zincirler (`EMI-1001` <- `EMI-1002`, `EMI-1003` vb.) kuruldu.
2. **Backend & Tooling:**
   - `KayitTipi` enum'ı oluşturuldu. `TacticalEmissionEntity`, `TacticalComintEntity`, JPA Repository'leri ve 4 adet DTO güncellendi.
   - `RadarEmissionTools` ve `ComintEmissionTools` üzerinden `kayitTipi` filtresi, sıralama ve çift yönlü detay sorguları eklendi.
   - `TacticalController` REST uç noktaları (`/emissions`, `/comints`, `/emissions/{id}`, `/comint/{id}`) güncellendi.
3. **Frontend (React/AntD):**
   - Radar ve COMINT tablolarına `[KESTİRİM]` ve `[HAM LOB]` etiketleri ile `Kayıt Niteliği` filtre açılır menüsü eklendi.
   - Detay çekmecesinde (`DetailDrawer`) doktrin panelleri geliştirildi: Kestirim incelenirken katkı veren LOB'lar tıklanabilir etiketler olarak listelenmekte; bir LOB incelenirken bağlı olduğu kestirime doğrudan tıklanarak geçiş yapılabilmektedir.

