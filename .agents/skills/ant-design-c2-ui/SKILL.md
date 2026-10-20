---
name: ant-design-c2-ui
description: >-
  Ant Design (v5.x), React ve modern JavaScript/TypeScript kullanarak kurumsal Komuta Kontrol (C2),
  telemetri ve taktiksel savunma kullanıcı arayüzleri geliştirme rehberi. Yüksek performanslı sanal tablolar,
  ağaç/hiyerarşi bileşenleri, detay çekmeceleri (drawer), askeri koyu tema (MIL-Dark), WebSocket/STOMP
  canlı veri akışları, ECharts taktiksel grafik hayat döngüsü (chart.clear, replaceMerge, legendselectchanged)
  ve gerçek zamanlı state yönetimi standartlarını kapsar.
---

# Komuta Kontrol (C2) ve Taktik Savunma Sistemleri İçin Ant Design & React Rehberi

Bu yetenek dokümanı, **Ant Design (antd v5.x)**, **React** ve **modern JavaScript/TypeScript** kullanarak görev kritik Elektronik Harp (EH / C2), telemetri izleme ve taktik karar destek arayüzleri geliştirmek için gereken tasarım sistemini ve performans kalıplarını tanımlar.

---

## 1. Askeri Koyu Tema (MIL-Dark ConfigProvider)

Taktik harekât merkezlerinde (C2 TOC) ekranlar göz yorgunluğunu önlemeli, düşük ortam ışığında yüksek kontrast sunmalı ve kritik tehditleri (kırmızı/turuncu) anında öne çıkarmalıdır.

```tsx
import React from 'react';
import { ConfigProvider, theme } from 'antd';

export const TacticalThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#38bdf8',     // Taktik Camgöbeği / Mavi
          colorSuccess: '#10b981',     // Dost Unsur / Güvenli
          colorWarning: '#f97316',     // Şüpheli / Muhtemel Düşman
          colorError: '#ef4444',       // Kritik Tehdit / Düşman
          colorInfo: '#a855f7',        // Tarafsız / Sivil (Taktiksel Mor)
          colorBgBase: '#0b1329',      // Derin Taktik Gece Mavisi
          colorBgContainer: '#0f172a', // Kart, Panel ve Tablo Arka Planı
          colorBorder: '#1e293b',      // İnce Askeri Ayırıcı Çizgiler
          fontFamily: "'Inter', 'JetBrains Mono', 'Roboto Mono', sans-serif",
          borderRadius: 4,             // Keskin askeri köşeler (yuvarlak değil)
        },
      }}
    >
      {children}
    </ConfigProvider>
  );
};
```

---

## 2. Yüksek Performanslı Taktik Tablolar (AntD Table)

On binlerce radar veya telsiz emisyonunun aktığı ortamlarda DOM tıkanmasını önlemek için sanal kaydırma (virtual scroll) veya sayfalama uygulanmalıdır.

### A. Tehdit ve Hiyerarşi Rozetleri (Badges & Tags)
```tsx
import { Tag, Badge } from 'antd';

export const renderIdentityTag = (identity: string) => {
  switch (identity) {
    case 'DUSMAN':
      return <Tag color="#ef4444" style={{ fontWeight: 600 }}>DÜŞMAN</Tag>;
    case 'MUHTEMEL_DUSMAN':
      return <Tag color="#f97316">MUHT. DÜŞMAN</Tag>;
    case 'BILINMEYEN':
      return <Tag color="#eab308">BİLİNMEYEN</Tag>;
    case 'DOST':
      return <Tag color="#10b981">DOST</Tag>;
    case 'TARAFSIZ':
      return <Tag color="#a855f7" style={{ fontWeight: 600 }}>TARAFSIZ</Tag>;
    default:
      return <Tag color="#64748b">{identity}</Tag>;
  }
};
```

### B. Başıboş vs. Bağlı LOB Gösterimi
```tsx
export const renderLobStatus = (val: string, record: any) => {
  if (val === 'HAM_LOB') {
    return (
      <div>
        <Tag color="orange" style={{ fontWeight: 600, fontSize: 11 }}>HAM LOB</Tag>
        {record.kestirimId ? (
          <div style={{ fontSize: 10, color: '#38bdf8', marginTop: 2 }}>
            Bağlı: <Tag color="blue" style={{ fontSize: 9, padding: '0 4px', margin: 0 }}>{record.kestirimId}</Tag>
          </div>
        ) : (
          <div style={{ fontSize: 10, color: '#fbbf24', marginTop: 2 }}>
            ⚠️ Başıboş LOB
          </div>
        )}
      </div>
    );
  }
  return <Tag color="cyan" style={{ fontWeight: 600, fontSize: 11 }}>KESTİRİM</Tag>;
};
```

---

## 3. ECharts Taktik Görselleştirme Hayat Döngüsü & Kritik Çözümler

ECharts kütüphanesini React içinde kullanırken karşılaşılan hayati tuzaklar ve kesin çözümleri:

### ⚠️ Gotcha 1: Serilerin Tuvalde Asılı Kalması (`chart.clear()` ve `replaceMerge`)
* **Problem:** Kullanıcı grafikte örneğin `DOST` (yeşil) serisini açıp sonra kapattığında, ECharts serileri birleştirdiği (merge ettiği) için yeşil barlar tuvalden silinmez ve ekranda kalmaya devam eder.
* **Kesin Çözüm:** Her `setOption` öncesinde mutlaka `chart.clear()` çağrılmalı ve `replaceMerge` parametresi verilmelidir:
```javascript
// Tuvaldeki eski serileri ve lejantı tamamen süpür
chart.clear();

chart.setOption(option, {
  notMerge: true,
  replaceMerge: ['series', 'legend', 'xAxis', 'yAxis']
});
```

### ⚠️ Gotcha 2: Lejantın Çift Yönlü Senkronizasyonu (`legendselectchanged`)
* **Problem:** Kullanıcı grafik üzerindeki lejant kutucuğuna (örn. "DOST") tıkladığında ECharts seriyi yerel olarak gizler ancak üstteki filtre butonları (`Tag`) ve tablo güncellenmez.
* **Kesin Çözüm:** `legendselectchanged` olayını dinleyip React state fonksiyonunu tetiklemek:
```javascript
chart.off('legendselectchanged');
chart.on('legendselectchanged', (params) => {
  const LABEL_TO_KEY = {
    'DÜŞMAN': 'DUSMAN',
    'MUHT. DÜŞMAN': 'MUHTEMEL_DUSMAN',
    'BİLİNMEYEN': 'BILINMEYEN',
    'DOST': 'DOST',
    'TARAFSIZ': 'TARAFSIZ'
  };
  const key = LABEL_TO_KEY[params.name] || params.name;
  if (key) {
    toggleIdentity(key);
  }
});
```

### ⚠️ Gotcha 3: Tek Seride Lejantın Açıkça Kapatılması
* Tek seri varken `legend: undefined` geçilirse ECharts eski çoklu seriden kalan lejantı silmez. Açıkça kapatılmalıdır:
```javascript
legend: {
  show: !isSingleIdentity,
  data: legendNames,
  top: 4,
  right: 16,
  textStyle: { color: '#cbd5e1', fontSize: 11 }
}
```

### ⚠️ Gotcha 4: Modal / Dialog İçinde ECharts Tuvalinin Boş (0x0) Kalması
* **Problem:** Ant Design `Modal` bileşeni portal (`document.body`) kullandığından, `modalVisible` `true` olduğu ilk render döngüsünde `modalChartRef.current` henüz `null`'dur. Basit `useEffect([modalVisible])` kontrolünde `if (!modalChartRef.current) return;` erken döner ve `useRef` değişimi yeni render tetiklemediği için grafik tuvali hiç oluşturulmaz (ekranda sadece boş koyu mavi modal penceresi kalır). Ayrıca modal açılış animasyonu (`scale`) sırasında tuval 0x0 piksel boyutlanır.
* **Kesin Çözüm (Üçlü Garanti Kalıbı):**
  1. **Callback Ref:** Modal içindeki `div` DOM'a eklenir eklenmez yakalamak için `ref={(el) => { modalChartRef.current = el; if(el && modalVisible) renderModal(el); }}` kullanılmalıdır.
  2. **`afterOpenChange` Olayı:** Modal animasyonu tamamlanıp kutu tam boyutuna ulaştığında `<Modal afterOpenChange={(open) => { if (open) renderModal(); }}>` ile boyutlama tetiklenmelidir.
  3. **Aşamalı `resize()`:** Render fonksiyonu içinde `50ms` ve `250ms` aralıklarla `chart.resize()` yinelenmelidir.


### 💡 Bar Üstü Etiketinde Yüzde + Temas Sayısı Gösterimi
Operatör zaman filtresini değiştirdiğinde (1 saat $\rightarrow$ 24 saat), frekans kapsama yüzdesi aynı kalsa bile temas sayısındaki 5 katlık artışı hemen görebilmelidir:
```javascript
label: {
  show: true,
  position: 'top',
  formatter: (p) => {
    if (!p || p.value <= 0) return '';
    const cnt = p.data?.detailInfo?.emissionCount;
    const cStr = cnt != null && cnt > 0 ? (cnt >= 1000 ? (cnt / 1000).toFixed(1) + 'k' : cnt) : null;
    return cStr ? `%${p.value}\n(${cStr})` : `%${p.value}`;
  },
  color: '#f8fafc',
  fontSize: 9,
  fontWeight: 'bold',
  lineHeight: 12
}
```

---

## 4. Taktiksel Filtreleme Düğmeleri (Checkable Tag & Rozet)

Filtre butonlarının açık/kapalı durumunu anında hissettirmek için taktiksel onay rozetleri kullanılmalıdır:

```tsx
{Object.keys(IDENTITY_CONFIG).map(idKey => {
  const cfg = IDENTITY_CONFIG[idKey];
  const isSel = selectedIdentities.includes(idKey);
  return (
    <span
      key={idKey}
      onClick={() => handleToggle(idKey)}
      style={{
        fontSize: 10,
        padding: '2px 8px',
        borderRadius: 4,
        cursor: 'pointer',
        userSelect: 'none',
        border: isSel ? `1px solid ${cfg.color}` : '1px solid #334155',
        backgroundColor: isSel ? `${cfg.color}26` : 'transparent',
        color: isSel ? cfg.color : '#94a3b8',
        fontWeight: isSel ? 700 : 400,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4
      }}
    >
      <span>{isSel ? '✓' : '○'}</span>
      {cfg.label}
    </span>
  );
})}
```

---

## 5. Canlı Telemetri ve STOMP / WebSocket Entegrasyonu

Canlı radar ve COMINT verisi akarken React state'inin kilitlenmemesi için:
1. **Fonksiyonel State Güncellemesi:**
   ```tsx
   setSelectedIdentities(prev => {
     const next = prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key];
     return next.length === 0 ? ['DUSMAN'] : next;
   });
   ```
2. **STOMP Abonelik İptali (Cleanup):**
   `useEffect` temizleme bloğunda abonelikler mutlaka `subscription.unsubscribe()` ile sonlandırılmalı, bellek sızıntıları önlenmelidir.
