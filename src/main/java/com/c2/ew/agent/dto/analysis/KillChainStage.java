package com.c2.ew.agent.dto.analysis;

public enum KillChainStage {
    ENGAGE("ENGAGE (Ateş & Güdüm)", "#dc2626", 5),
    TARGET("TARGET (Hedefleme & Atış Kontrol)", "#ea580c", 4),
    TRACK("TRACK (Hassas Takip)", "#d97706", 3),
    FIX("FIX (Teşhis & Konumlama)", "#ca8a04", 2),
    FIND("FIND (Tespit & Erken İhbar)", "#16a34a", 1);

    private final String description;
    private final String badgeColor;
    private final int order;

    KillChainStage(String description, String badgeColor, int order) {
        this.description = description;
        this.badgeColor = badgeColor;
        this.order = order;
    }

    public String getDescription() { return description; }
    public String getBadgeColor() { return badgeColor; }
    public int getOrder() { return order; }
}
