---
name: ant-design-c2-ui
description: >-
  Expert guidance for designing and implementing modern enterprise Command and Control (C2),
  telemetry, and tactical defense user interfaces using Ant Design (v5.x), React, and modern
  JavaScript/TypeScript. Covers high-performance data tables with virtual scroll, tree/hierarchy
  components, drawer inspector panels, status badges, military dark themes, WebSocket/STOMP
  live feeds, and real-time state management.
---

# Ant Design & Modern JS/TS for Tactical C2 Systems

This skill guides the design, implementation, and optimization of mission-critical defense, telemetry, and Electronic Warfare (EW / C2) user interfaces using **Ant Design (antd v5.x)**, **React**, and **TypeScript/JavaScript**.

---

## 1. Tactical Dark Military Theme Configuration (ConfigProvider)

In tactical operations centers, screens must avoid eye fatigue, maintain high contrast under low ambient light, and highlight threats immediately.

```tsx
import React from 'react';
import { ConfigProvider, theme } from 'antd';

export const TacticalThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#1890ff',     // Taktik Mavi (Dost)
          colorSuccess: '#52c41a',     // Güvenli / Teyitli
          colorWarning: '#fa8c16',     // Şüpheli / Muhtemel Düşman
          colorError: '#f5222d',       // Kritik Tehdit / Düşman / Atış Kontrol
          colorInfo: '#13c2c2',        // Bilgi / Link-16 / Harici
          colorBgBase: '#0b132b',      // Derin Taktik Lacivert
          colorBgContainer: '#1c2541', // Kart ve Tablo Arka Planı
          colorBorder: '#3a506b',      // İnce Askeri Ayırıcılar
          fontFamily: "'Inter', 'Roboto Mono', -apple-system, sans-serif",
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

## 2. High-Performance Virtualized Defense Tables (Table)

When handling 10,000+ radar or COMINT emissions, regular DOM rendering causes lag. Always use pagination or virtualized scrolling.

### A. Identification Badges & Tag Component
```tsx
import { Tag, Badge } from 'antd';

export const renderThreatTag = (teshis: string) => {
  switch (teshis) {
    case 'DUSMAN':
      return <Tag color="#f5222d" style={{ fontWeight: 600 }}>DÜŞMAN</Tag>;
    case 'MUHTEMEL_DUSMAN':
      return <Tag color="#fa8c16">MUHTEMEL DÜŞMAN</Tag>;
    case 'DOST':
      return <Tag color="#52c41a">DOST</Tag>;
    case 'TARAFSIZ':
      return <Tag color="#1890ff">TARAFSIZ</Tag>;
    default:
      return <Tag color="#d9d9d9">BİLİNMEYEN</Tag>;
  }
};
```

### B. Table Columns with Custom Sorting, Filtering & Formats
```tsx
import { Table, Space, Button, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';

export interface TacticalEmissionRow {
  id: string;
  radarAdi: string;
  teshis: string;
  frekansMhz: number;
  kayitTipi: 'KESTIRIM_ELIPS' | 'HAM_LOB';
  cocukSayisi?: number;
  sonTespitZamani: string;
}

export const emissionColumns = (onInspect: (id: string) => void): ColumnsType<TacticalEmissionRow> => [
  {
    title: 'Kayıt ID',
    dataIndex: 'id',
    key: 'id',
    width: 140,
    render: (id) => <span style={{ fontFamily: 'monospace', color: '#6fffe9' }}>[{id}]</span>,
  },
  {
    title: 'Radar / Unsur Adı',
    dataIndex: 'radarAdi',
    key: 'radarAdi',
    sorter: (a, b) => a.radarAdi.localeCompare(b.radarAdi),
  },
  {
    title: 'Teşhis',
    dataIndex: 'teshis',
    key: 'teshis',
    render: renderThreatTag,
    filters: [
      { text: 'DÜŞMAN', value: 'DUSMAN' },
      { text: 'MUHTEMEL DÜŞMAN', value: 'MUHTEMEL_DUSMAN' },
      { text: 'DOST', value: 'DOST' },
    ],
    onFilter: (value, record) => record.teshis === value,
  },
  {
    title: 'Frekans (MHz)',
    dataIndex: 'frekansMhz',
    key: 'frekansMhz',
    align: 'right',
    sorter: (a, b) => a.frekansMhz - b.frekansMhz,
    render: (val) => val ? `${val.toLocaleString()} MHz` : '-',
  },
  {
    title: 'Hiyerarşi / Durum',
    key: 'durum',
    render: (_, r) => {
      if (r.kayitTipi === 'KESTIRIM_ELIPS') {
        const cc = r.cocukSayisi || 0;
        return cc > 0 ? (
          <Badge status="processing" color="#52c41a" text={`Çocuklu (${cc} LOB)`} />
        ) : (
          <Badge status="default" color="#1890ff" text="Çocuksuz (Doğrudan/Veri Bağı)" />
        );
      }
      return <Badge status="warning" text="Ham LOB" />;
    },
  },
  {
    title: 'İşlem',
    key: 'action',
    width: 100,
    render: (_, r) => (
      <Button size="small" type="primary" ghost onClick={() => onInspect(r.id)}>
        İncele
      </Button>
    ),
  },
];
```

---

## 3. Tree & Hierarchy Component for Ellipse-LOB Association

Visualize parent-child relationships using `Tree` or `Table` expandable rows:

```tsx
import React from 'react';
import { Table } from 'antd';

export const HierarchyExpandableTable: React.FC<{ data: any[] }> = ({ data }) => {
  return (
    <Table
      columns={parentColumns}
      expandable={{
        expandedRowRender: (record) => (
          <Table
            columns={childLobColumns}
            dataSource={record.childLobs}
            pagination={false}
            size="small"
          />
        ),
        rowExpandable: (record) => record.childLobs && record.childLobs.length > 0,
      }}
      dataSource={data}
    />
  );
};
```

---

## 4. Emitter Telemetry Drawer Inspector (Drawer & Descriptions)

When an operator clicks "İncele" on a target, use Ant Design's `Drawer` to open a high-density parameters panel:

```tsx
import { Drawer, Descriptions, Tag, Progress } from 'antd';

export const EmitterDrawer: React.FC<{ open: boolean; onClose: () => void; emitter: any }> = ({
  open,
  onClose,
  emitter,
}) => {
  if (!emitter) return null;

  return (
    <Drawer
      title={`Hedef Detay İnceleme: [${emitter.id}] ${emitter.radarAdi || emitter.cagriAdi}`}
      placement="right"
      width={560}
      onClose={onClose}
      open={open}
      styles={{ body: { backgroundColor: '#1c2541' } }}
    >
      <Descriptions bordered size="small" column={2}>
        <Descriptions.Item label="Teşhis">{renderThreatTag(emitter.teshis)}</Descriptions.Item>
        <Descriptions.Item label="Kayıt Tipi">{emitter.kayitTipi}</Descriptions.Item>
        <Descriptions.Item label="Frekans Aralığı">
          {emitter.minFrekansMhz} - {emitter.maxFrekansMhz} MHz
        </Descriptions.Item>
        <Descriptions.Item label="PRI / PW">
          {emitter.priMicroSec} µs / {emitter.pwMicroSec} µs
        </Descriptions.Item>
        <Descriptions.Item label="Modülasyon">{emitter.modulasyon || 'SABİT'}</Descriptions.Item>
        <Descriptions.Item label="Elektronik Taarruz (ET)">
          {emitter.etUygulamaDurumu ? <Tag color="red">AKTİF KARIŞTIRMA</Tag> : <Tag color="green">YOK</Tag>}
        </Descriptions.Item>
        <Descriptions.Item label="Hedef Konum (Lat/Lon)" span={2}>
          {emitter.yayinEnlem?.toFixed(4)} K, {emitter.yayinBoylam?.toFixed(4)} D
        </Descriptions.Item>
        <Descriptions.Item label="Hata Elipsi (CEP)" span={2}>
          {emitter.yayinSemiMajorMeters} m x {emitter.yayinSemiMinorMeters} m
        </Descriptions.Item>
        <Descriptions.Item label="Operatör Notu" span={2}>
          {emitter.operatorNotu || 'Standart taktik iz.'}
        </Descriptions.Item>
      </Descriptions>
    </Drawer>
  );
};
```

---

## 5. Real-Time WebSocket / STOMP Updates in React

To handle live streaming telemetry without full table re-renders:

```ts
import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';

export function useTacticalStream(onEmissionReceived: (emission: any) => void) {
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8082/ws-tactical',
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe('/topic/live-emissions', (message) => {
          if (message.body) {
            const data = JSON.parse(message.body);
            onEmissionReceived(data);
          }
        });
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [onEmissionReceived]);
}
```

---

## 6. Best Practices for Ant Design in Defense Apps

1. **Avoid Layout Shift:** Always reserve fixed pixel heights for tables and charts.
2. **Dense Information Display:** Use `size="small"` on `Table`, `Descriptions`, and `Form` to fit maximum intelligence per viewport.
3. **Monospace for Identifiers:** All coordinates, frequencies, timestamps, and IDs should be styled with `font-family: monospace` for instant vertical alignment.
4. **Debounced Search:** Always debounce text searches on 100K+ tables by 300ms using `useDebounce`.
