package com.c2.ew.agent.dto.analysis;

public enum ThreatLevel {
    SEVIYE_4_KRITIK("SEVİYE 4 - KRİTİK ANGAJMAN", "#ef4444", 4),
    SEVIYE_3_YUKSEK("SEVİYE 3 - HEDEF TAKİP & HEDEFLEME", "#f97316", 3),
    SEVIYE_2_ORTA("SEVİYE 2 - ERKEN UYARI & TEŞHİS", "#eab308", 2),
    SEVIYE_1_DUSUK("SEVİYE 1 - RUTİN GÖZETLEME", "#22c55e", 1);

    private final String label;
    private final String color;
    private final int severity;

    ThreatLevel(String label, String color, int severity) {
        this.label = label;
        this.color = color;
        this.severity = severity;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
    public int getSeverity() { return severity; }
}
