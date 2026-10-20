---
name: spring-ai-agent-craft
description: >-
  Spring Boot 3.3+ ve Spring AI 1.x ile deterministik, kurumsal ve askeri yapay zeka ajanları (Copilot)
  inşa etmek için en iyi uygulamalar ve mimari kalıplar. @Tool / Function Calling mekanizmaları,
  kayıt (record) DTO serileştirmesi, MessageChatMemoryAdvisor oturum yönetimi, JPA native query çoklu kolon
  dizi dönüşümü (gotcha çözümleri), Spektrum Doluluk analizi entegrasyonu ve üstel geri çekilmeli (retry)
  hata toleransı kalıplarını kapsar.
---

# Spring AI Ajan Mimarisi ve Fonksiyon Çağrısı (Function Calling) Rehberi

Bu yetenek dokümanı, **Spring Boot 3.3+** ve **Spring AI 1.x** (Gemini / OpenAI altyapıları) kullanarak görev kritik, deterministik ve yüksek performanslı askeri Elektronik Harp (EH / C2) yapay zeka ajanları geliştirmek için gereken mimari standartları ve kodlama kalıplarını sunar.

---

## 1. ChatClient Akıcı (Fluent) Mimarisi ve Araç Bağlama (Tool Binding)

Spring AI 1.x sürümünde ajan yapılandırması `ChatClient.Builder` üzerinden akıcı (fluent) arayüz ile kurulmalıdır. Sistem talimatı (System Directive), araçlar (`@Tool` sınıfları) ve oturum belleği (ChatMemory) kurucu enjeksiyonu ile tek bir noktada yapılandırılır.

```java
package com.c2.ew.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TacticalAgentService {

    private final ChatClient chatClient;

    public TacticalAgentService(
        ChatClient.Builder chatClientBuilder,
        @Autowired(required = false) ChatMemory chatMemory,
        RadarEmissionTools radarTools,
        ComintEmissionTools comintTools
    ) {
        ChatClient.Builder builder = chatClientBuilder
            .defaultSystem(SYSTEM_DIRECTIVE)
            .defaultTools(radarTools, comintTools);

        if (chatMemory != null) {
            builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );
        }

        this.chatClient = builder.build();
    }
}
```

* **Önemli Kural:** `defaultTools` metoduna servis nesneleri doğrudan geçilebilir. Spring AI, sınıf içindeki `@Tool` anotasyonlu metotları otomatik tarayarak LLM için JSON şemasına (Function Specification) dönüştürür.

---

## 2. Çok Turlu Oturum Belleği Yönetimi (`conversationId`)

Operatör ile ajan arasındaki çok turlu (multi-turn) taktiksel diyalogların birbirine karışmaması için her oturum kesinlikle benzersiz bir `conversationId` (örneğin UUID veya kokpit oturum ID'si) ile yalıtılmalıdır:

```java
public String askCopilot(String userPrompt, String conversationId) {
    return chatClient.prompt()
        .advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
        .user(userPrompt)
        .call()
        .content();
}
```

### ⚠️ Kritik DTO Parametre Kuralı
Spring Boot REST Controller katmanında gelen JSON gövdesi `record` veya DTO ile eşleştirilirken alan adları büyük/küçük harf duyarlı olarak birebir örtüşmelidir:
* Java record: `public record AgentChatRequest(String userPrompt, String conversationId) {}`
* JSON gövdesi: `{"userPrompt": "Spektrum doluluk oranını göster", "conversationId": "sess-102"}`
* Eğer DTO `userPrompt` beklerken frontend `prompt` gönderirse alan `null` kalır ve `IllegalArgumentException: User prompt must not be blank` hatası oluşur.

---

## 3. Spring AI `@Tool` Fonksiyon Tanımlama Kuralları

LLM'in araçları hatasız ve doğru zamanda tetiklemesi için şu kurallar zorunludur:

1. **Açıklayıcı `@Tool` Tanımı (`description`):**
   Açıklama sadece aracın ne yaptığını değil, **LLM'in hangi kullanıcı sorularında bu aracı çağırması gerektiğini** de açıkça belirtmelidir.
   ```java
   @Tool(description = "Radar frekans spektrumunun doluluk ve bant kullanım yoğunluğunu hesaplar. " +
                       "Kullanıcı 'spektrum doluluk', 'frekans kullanımı', 'radar doluluk durumu', " +
                       "'spektrum analizi' gibi sorular sorduğunda bu fonksiyonu çağırın.")
   public String calculateSpectrumOccupancy(
       @ToolParam(description = "Zaman aralığı: SON_1_SAAT, SON_6_SAAT, SON_24_SAAT, SON_1_HAFTA, ALL") String timeRange,
       @ToolParam(description = "Teşhis kimliği filtresi: DUSMAN, DOST, BILINMEYEN, TARAFSIZ, HEPSI") String teshisKimlik
   ) { ... }
   ```

2. **İlkel Tip (Primitive) Yerine Sarmalayıcı (Wrapper) Tipler:**
   Parametrelerde `int`, `double`, `boolean` yerine daima `Integer`, `Double`, `Boolean` kullanılmalıdır. Böylece operatör opsiyonel parametre belirtmediğinde LLM `null` gönderebilir ve metot patlamaz.

3. **Denetim ve Loglama İçin `ThreadLocal` Kullanımı:**
   Ajana gelen bir sorguda hangi araçların tetiklendiğini REST yanıtında veya UI telemetrisinde gösterebilmek için `ThreadLocal` çağrı günlüğü tutulmalıdır:
   ```java
   public class RadarEmissionTools {
       private static final ThreadLocal<List<String>> CALL_LOGS = ThreadLocal.withInitial(ArrayList::new);

       protected void logCall(String toolName, Object... args) {
           CALL_LOGS.get().add(toolName + "(" + Arrays.toString(args) + ")");
       }

       public static List<String> drainCallLogs() {
           List<String> logs = new ArrayList<>(CALL_LOGS.get());
           CALL_LOGS.get().clear();
           return logs;
       }
   }
   ```

---

## 4. Spektrum Doluluk Analizi & Ajan Entegrasyonu (Interval Union)

Elektronik Harp'te radarlar tek bir frekans noktası yerine bir frekans aralığında (`minFrekansMhz` - `maxFrekansMhz`) çalışır. Spektrum doluluk hesaplamasında:

1. **Sabit Dar Bant Dilimleme (Bin Step, örn. 50 MHz):**
   Frekans ekseni sabit aralıklara bölünür (`[4500-4550]`, `[4550-4600]`, `[9200-9250]`).
2. **Geometrik Aralık Birleşimi (Interval Union):**
   Aynı kanala basan birden fazla radarın frekans aralıkları üst üste çakışsa bile kanal genişliğini aşmayacak şekilde birleştirilir:
   $$\text{Doluluk (\%)} = \frac{\text{Aralıkların Birleşimi (Net Kaplanan MHz)}}{\text{Kanal Bant Genişliği (50 MHz)}} \times 100$$
3. **Markdown Tablosu Çıktısı:**
   Ajan aracı çalıştırdığında kullanıcıya doğrudan okunaklı bir Markdown tablosu ve aynı anda ön yüzde grafiği tetikleyecek yapılandırılmış veri DTO'su döndürmelidir.

---

## 5. JPA Native Query Çok Kolonlu Dizi Dönüşüm Tuzağı (Gotcha & Çözümü)

Spring Data JPA'da varlık (entity) eşlemesi olmayan çok kolonlu yerel (native) sorgularda:
* **Tuzak:** Tek satırlı sorgularda Spring Data JPA `Object[]` yerine `counts[0]` elemanının içine bir `Object[]` dizisi daha sararak (`[Ljava.lang.Object;`) dönebilir. Doğrudan `(Number) result[0]` yapıldığında `ClassCastException` fırlar.
* **Garantili ve Güvenli Çözüm:**
```java
Object[] raw = repo.getHierarchyCounts(start, end);
Object[] row = raw;
if (raw != null && raw.length > 0 && raw[0] instanceof Object[] innerRow) {
    row = innerRow;
}

long totalCount = extractLong(row, 0);
long childedCount = extractLong(row, 1);

private long extractLong(Object[] arr, int index) {
    if (arr == null || index >= arr.length || arr[index] == null) return 0L;
    if (arr[index] instanceof Number num) return num.longValue();
    try {
        return Long.parseLong(arr[index].toString().trim());
    } catch (Exception e) {
        return 0L;
    }
}
```

---

## 6. Hata Dayanıklılığı: Üstel Geri Çekilme (Exponential Backoff Retry)

Uzak LLM servisleri (Gemini API / OpenAI) ağ gecikmeleri veya anlık hız kısıtlamaları (`HTTP 429 Too Many Requests`) yaşayabilir. Ajan servis katmanında mutlaka üstel geri çekilmeli yeniden deneme (retry) döngüsü bulunmalıdır:

```java
int maxRetries = 3;
for (int attempt = 1; attempt <= maxRetries; attempt++) {
    try {
        return chatClient.prompt().user(prompt).call().content();
    } catch (Exception ex) {
        if (attempt == maxRetries) {
            log.error("LLM çağrısı {} deneme sonrasında başarısız oldu: {}", maxRetries, ex.getMessage());
            throw ex;
        }
        long waitTimeMs = 1200L * attempt;
        log.warn("LLM API hatası, {} ms sonra tekrar deneniyor (Deneme: {}/{})", waitTimeMs, attempt, maxRetries);
        Thread.sleep(waitTimeMs);
    }
}
```
