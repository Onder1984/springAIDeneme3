package com.c2.ew.agent.service;

import com.c2.ew.agent.dto.PaginationMetadata;
import com.c2.ew.agent.dto.TacticalComintSummaryDto;
import com.c2.ew.agent.dto.TacticalEmissionSummaryDto;
import com.c2.ew.agent.dto.TacticalQueryRequest;
import com.c2.ew.agent.dto.TacticalResponseDto;
import com.c2.ew.agent.geojson.TacticalGeoJsonBuilder;
import com.c2.ew.agent.tools.ComintEmissionTools;
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
        Görevin, taktik sahadaki:
        1. RADAR EH (ELINT / RESM - 37 Parametreli Radar Emisyonları): Kestirim elipsleri, kerteriz yön hatları, frekans/PRI/PW/ATP zarfları, HSS mevzileri, taciz ve elektronik taarruz (ET) durumları.
        2. MUHABERE EH (COMINT / CESM - 30 Parametreli Telsiz ve Haberleşme Yayınları): HF/VHF telsiz ağları, kriptolu haberleşmeler, darbe patlama (burst), karıştırma gayda/gürültü, lisanlar (Arapça, Rusça, İngilizce, Türkçe vb.), çağrı adları (callsign link analizi), DMR/D-Star protokolleri.
        
        UYULMASI ZORUNLU KATI KURALLAR:
        1. DETERMINIZM: Asla kafandan koordinat, frekans, radar adı, telsiz çağrı adı veya sayı uydurma. Tüm verileri mutlaka sana verilen araçları çağırarak çek.
        2. DOĞRU ARAÇ SEÇİMİ VE ÖZETLEME:
           - Genel taktik durum veya makro resim istendiğinde:
             * Radarlar için 'getTacticalMacroStats()' aracını çağır.
             * Telsiz/muhabere için 'getComintMacroStats()' aracını çağır.
           - Kritik tehditler:
             * Düşman radarları / HSS dendiğinde 'queryPriorityThreats(limit=15)' aracını kullan.
             * Düşman telsizleri / Kriptolu / Karıştırma yayınları dendiğinde 'queryPriorityComintThreats(limit=15)' aracını kullan.
           - Belirli filtre veya frekans/PRI/PW aralığı arandığında:
             * Radar için 'queryEmissionsPaged(...)'
             * Muhabere için 'queryComintEmissionsPaged(...)'
           - Telsiz Ağı ve Bağlantı (Link) Analizi:
             * Bir çağrı adının (örn. Kartal-1, Volga-04) kimlerle konuştuğunu bulmak için 'queryComintCallsignNetwork(cagriAdi)' aracını çağır.
           - Tekil hedef detayları:
             * Radar için 'getEmissionDetails(emissionId)'
             * Muhabere için 'getComintDetails(comintId)'
        3. ARALIKLI SORGULAMA KURALI (INTERVAL OVERLAP):
           - Operatör frekans (örn. '8500-10000 MHz' veya '150-170 MHz VHF'), PRI (örn. '500-1200 µs'), PW veya ATP aralığı sorduğunda:
             * minFrekansMhz, maxFrekansMhz gibi parametreleri doldur.
        4. ÇOKLU TUR SOHBET VE AKILLI SAYFALAMA (NEXT-PAGE / PREV-PAGE):
           - Kullanıcı 'Sonraki sayfayı ver', 'bana sonraki sayfayı da ver', 'Devamını getir' dediğinde:
             * Radar sorgusu devam ediyorsa 'getNextPageOfEmissions()' aracını çağır!
             * Muhabere sorgusu devam ediyorsa 'getNextPageOfComintEmissions()' aracını çağır!
             * Bu araçlar önceki filtreleri oturum hafızasından hatırlar ve bir sonraki sayfayı (page + 1) çeker.
           - 'Önceki sayfaya dön' dendiğinde ilgili 'getPrevPageOf...' aracını çağır.
        5. ÇIKTI FORMATI:
           - Yanıtlarını şık askeri başlıklar, markdown tabloları ve madde imleriyle sun.
             ## TAKTİK OPERASYONEL BRİFİNG
             ## TEŞHİS VE TEHDİT DEĞERLENDİRMESİ (RADAR & MUHABERE)
             ## HABERLEŞME AĞLARI VE ELEKTRONİK TAARRUZ DURUMU
             ## HAREKÂT TAVSİYELERİ
        """;

    private final ChatClient chatClient;
    private final TacticalGeoJsonBuilder geoJsonBuilder;
    private final RadarEmissionTools radarTools;
    private final ComintEmissionTools comintTools;

    public TacticalAgentService(
        ChatClient.Builder chatClientBuilder,
        @Autowired(required = false) ChatMemory chatMemory,
        TacticalGeoJsonBuilder geoJsonBuilder,
        RadarEmissionTools radarTools,
        ComintEmissionTools comintTools
    ) {
        this.geoJsonBuilder = geoJsonBuilder;
        this.radarTools = radarTools;
        this.comintTools = comintTools;

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

    public TacticalResponseDto processTacticalQuery(TacticalQueryRequest request) {
        String conversationId = (request != null && request.conversationId() != null && !request.conversationId().isBlank())
            ? request.conversationId()
            : "c2-tactical-session";

        RadarEmissionTools.setCurrentConversationId(conversationId);
        ComintEmissionTools.setCurrentConversationId(conversationId);

        String userPrompt = (request != null && request.userPrompt() != null) ? request.userPrompt() : "Sahadaki genel taktik durumu ve öncelikli hedefleri özetle.";
        log.info("Operatör Taktik Sorgusu İşleniyor [ConversationId: {}]: {}", conversationId, userPrompt);

        try {
            String operationalBriefing = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", conversationId))
                .user(userPrompt)
                .call()
                .content();

            List<String> executionLogs = new ArrayList<>();
            executionLogs.addAll(RadarEmissionTools.drainCallLogs());
            executionLogs.addAll(ComintEmissionTools.drainCallLogs());

            List<TacticalEmissionSummaryDto> queriedEmissions = RadarEmissionTools.drainQueriedEmissions();
            List<TacticalComintSummaryDto> queriedComints = ComintEmissionTools.drainQueriedComints();

            PaginationMetadata pagination = RadarEmissionTools.getPaginationMetadata();
            if (pagination == null) {
                pagination = ComintEmissionTools.getPaginationMetadata();
            }

            Map<String, Object> tacticalGeoJson = geoJsonBuilder.buildUnifiedFeatureCollection(queriedEmissions, queriedComints);

            log.info("Taktik analiz tamamlandı. Yapılan araç çağrıları: {}", executionLogs);
            return new TacticalResponseDto(operationalBriefing, tacticalGeoJson, executionLogs, pagination);

        } catch (Exception ex) {
            log.error("Taktik Sorgu İşleme Hatası: {}", ex.getMessage(), ex);
            return new TacticalResponseDto(
                "❌ **Taktik Analiz Hatası:** " + ex.getMessage() + "\nLütfen sistem telemetri loglarını ve API bağlantınızı kontrol ediniz.",
                Collections.emptyMap(),
                List.of("HATA: " + ex.getMessage()),
                null
            );
        }
    }
}
