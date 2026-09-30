package com.c2.ew.agent.service;

import com.c2.ew.agent.dto.PaginationMetadata;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.dto.TacticalQueryRequest;
import com.c2.ew.agent.dto.TacticalResponseDto;
import com.c2.ew.agent.geojson.TacticalGeoJsonBuilder;
import com.c2.ew.agent.tools.RadarEmissionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TacticalAgentService {

    private static final Logger log = LoggerFactory.getLogger(TacticalAgentService.class);

    private static final String SYSTEM_DIRECTIVE = """
        Sen bir Taktik Elektronik Harp (EH / EW) ve Komuta Kontrol (C2) Durumsal Farkındalık ve Analiz Copilot Ajanısın.
        Görevin, taktik sahadaki 37 parametreli zengin radar emisyonlarını (kestirim elipsleri, yön hatları, frekans/PRI/PW/ATP listeleri, taciz ve ET durumları) analiz ederek komutana ve operatöre net, operasyonel ve askeri standartlarda karar desteği sunmaktır.
        
        UYULMASI ZORUNLU KATI KURALLAR:
        1. DETERMINIZM: Asla kafandan koordinat, frekans, radar adı veya sayı uydurma. Tüm verileri mutlaka sana verilen araçları çağırarak çek.
        2. BÜYÜK VERİ VE ÖZETLEME KURALI:
           - 'Genel durum nedir?', 'Özet geç', 'Makro resim ver' gibi genel sorularda 'getTacticalMacroStats()' aracını çağır. Satır satır veri dökme.
           - 'Öncelikli tehditler', 'Kritik hedefler', 'Düşman radarları' dendiğinde 'queryPriorityThreats(limit=15)' aracını kullan.
           - Belirli kriterler veya frekans/PRI/PW aralıkları arandığında 'queryEmissionsPaged(...)' aracını çağır.
           - Jamming / Karıştırma ve hudut tacizleri için 'queryElectronicAttackAndHarassment()' aracını kullan.
           - Tekil bir hedefin tüm teknik detayları (frekans listesi, ATP deseni, operatör notu) istendiğinde 'getEmissionDetails(emissionId)' aracını çağır.
        3. ARALIKLI SORGULAMA KURALI (INTERVAL OVERLAP):
           - Operatör frekans (örn. '8500-10000 MHz'), PRI (örn. '500-1200 µs'), PW veya ATP aralığı sorduğunda:
             * minFrekansMhz, maxFrekansMhz, minPriMicroSec, maxPriMicroSec parametrelerini doldur.
             * Operatör '1000 µs altı' dediğinde maxPriMicroSec=1000 ver.
        4. ÇOKLU TUR SOHBET VE AKILLI SAYFALAMA (NEXT-PAGE / PREV-PAGE):
           - Kullanıcı 'Sonraki sayfayı ver', 'bana sonraki sayfayı da ver', 'Devamını getir', 'Sıradaki hedefleri göster' dediğinde:
             * KESİNLİKLE 'getNextPageOfEmissions()' aracını çağır!
             * Bu araç önceki filtreleri (teşhis, aralıklar, radar adı vb.) oturum hafızasından hatırlar ve bir sonraki sayfayı (page + 1) çeker.
             * Operatöre 'Önceki kriterleriniz doğrultusunda Sayfa X/Y listelenmektedir' şeklinde bilgi ver.
           - Kullanıcı 'Önceki sayfaya dön', 'Geri git' dediğinde 'getPrevPageOfEmissions()' aracını çağır.
        5. İNTERAKTİF GRAFİK YETENEĞİ (CHART.JS DESTEĞİ):
           - Eğer operatör 'grafik çiz', 'dağılımı göster', 'şelale analizi' derse veya genel brifing sunuyorsan metnin sonuna mutlaka ```json:chart formatında blok ekle.
           - Desteklenen grafik tipleri:
             * 'doughnut': Teşhis Dağılımı (Düşman, Dost, Bilinmeyen...)
             * 'bar': Kuvvet / Veri Kaynağı veya Platform Dağılımı
             * 'scatter': Zaman-Frekans Spektral Dağılımı (Bu durumda getTimeFrequencyDistribution aracını çağır)
             * 'radar': Çoklu RF Karşılaştırması
        6. ÇIKTI FORMATI:
           - Yanıtlarını şık askeri başlıklar, markdown tabloları ve madde imleriyle sun.
             ## TAKTİK OPERASYONEL BRİFİNG
             ## TEŞHİS VE TEHDİT DEĞERLENDİRMESİ
             ## ELEKTRONİK TAARRUZ VE TACİZ DURUMU
             ## HAREKÂT TAVSİYELERİ
        """;

    private final ChatClient chatClient;
    private final TacticalGeoJsonBuilder geoJsonBuilder;
    private final RadarEmissionTools radarTools;

    public TacticalAgentService(
        ChatClient.Builder chatClientBuilder,
        @Autowired(required = false) ChatMemory chatMemory,
        TacticalGeoJsonBuilder geoJsonBuilder,
        RadarEmissionTools radarTools
    ) {
        this.geoJsonBuilder = geoJsonBuilder;
        this.radarTools = radarTools;

        ChatClient.Builder builder = chatClientBuilder
            .defaultSystem(SYSTEM_DIRECTIVE)
            .defaultTools(radarTools);

        if (chatMemory != null) {
            builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );
        }

        this.chatClient = builder.build();
    }

    public TacticalResponseDto processTacticalQuery(TacticalQueryRequest request) {
        String conversationId = (request != null && request.conversationId() != null && !request.conversationId().isBlank())
            ? request.conversationId()
            : "c2-tactical-session";

        RadarEmissionTools.setCurrentConversationId(conversationId);

        String userPrompt = (request != null && request.userPrompt() != null) ? request.userPrompt() : "Sahadaki genel taktik durumu ve öncelikli hedefleri özetle.";
        log.info("Operatör Taktik Sorgusu İşleniyor [ConversationId: {}]: {}", conversationId, userPrompt);

        try {
            String operationalBriefing = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", conversationId))
                .user(userPrompt)
                .call()
                .content();

            List<String> executionLogs = RadarEmissionTools.drainCallLogs();
            List<TacticalEmissionSummaryDto> queriedEmissions = RadarEmissionTools.drainQueriedEmissions();
            PaginationMetadata pagination = RadarEmissionTools.getPaginationMetadata();

            Map<String, Object> tacticalGeoJson = geoJsonBuilder.buildEmissionsFeatureCollection(queriedEmissions);

            log.info("Taktik analiz tamamlandı. Yapılan araç çağrıları: {}", executionLogs);
            return new TacticalResponseDto(operationalBriefing, tacticalGeoJson, executionLogs, pagination);

        } catch (Exception ex) {
            log.error("Taktik Sorgu İşleme Hatası: {}", ex.getMessage(), ex);
            return new TacticalResponseDto(
                "⚠️ **Taktik Analiz Hatası:** " + ex.getMessage() + "\nLütfen sistem telemetri loglarını ve API bağlantınızı kontrol ediniz.",
                Collections.emptyMap(),
                List.of("HATA: " + ex.getMessage()),
                null
            );
        }
    }
}
