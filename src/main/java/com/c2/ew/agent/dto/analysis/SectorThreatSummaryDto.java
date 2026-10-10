package com.c2.ew.agent.dto.analysis;

import java.util.List;

public record SectorThreatSummaryDto(
    String bolge,
    String analizZamaniFormatted,
    int toplamTarananYayin,
    int kritikTehditSayisi,
    int yuksekTehditSayisi,
    int ortaTehditSayisi,
    int dusukTehditSayisi,
    ThreatLevel genelTehditSeviyesi,
    String genelHarekatOzeti,
    String comintKorelasyonNotu,
    List<ThreatItemDto> tehditler
) {}
