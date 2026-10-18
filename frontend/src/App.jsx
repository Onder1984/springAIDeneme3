import React, { useState } from 'react';
import { ConfigProvider, theme, Tabs, Layout, message } from 'antd';
import {
  MessageOutlined,
  RadarChartOutlined,
  SecurityScanOutlined
} from '@ant-design/icons';
import axios from 'axios';
import HeaderBar from './components/HeaderBar';
import CopilotChat from './components/CopilotChat';
import RadarTable from './components/RadarTable';
import ComintTable from './components/ComintTable';
import DetailDrawer from './components/DetailDrawer';

const { Content } = Layout;

export default function App() {
  const [activeTab, setActiveTab] = useState('chat');
  const [radarFilter, setRadarFilter] = useState('');
  const [comintFilter, setComintFilter] = useState('');

  // Drawer Durumu
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerType, setDrawerType] = useState('RADAR');
  const [drawerData, setDrawerData] = useState(null);
  const [drawerLoading, setDrawerLoading] = useState(false);

  // Tekil Hedef Detayını Çekme ve Drawer Açma
  const handleOpenDetail = async (id, explicitType) => {
    const isRadar = explicitType ? explicitType === 'RADAR' : id.startsWith('EMI');
    const type = isRadar ? 'RADAR' : 'COMINT';
    setDrawerType(type);
    setDrawerOpen(true);
    setDrawerLoading(true);

    try {
      const url = isRadar
        ? `/api/v1/tactical/emissions/${id}`
        : `/api/v1/tactical/comint/${id}`;
      const res = await axios.get(url);
      setDrawerData(res.data);
    } catch (err) {
      console.error('Detay çekme hatası:', err);
      message.error(`Hedef detayları alınamadı: ${id}`);
    } finally {
      setDrawerLoading(false);
    }
  };

  const handleNavigateTab = (tabKey, filter = '') => {
    setActiveTab(tabKey);
    if (tabKey === 'radar') {
      setRadarFilter(filter);
    } else if (tabKey === 'comint') {
      setComintFilter(filter);
    }
  };

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#0284c7',
          colorBgBase: '#080d1a',
          colorBgContainer: '#0f172a',
          colorBorder: '#1e293b',
          borderRadius: 6,
          fontFamily: "'Inter', sans-serif"
        }
      }}
    >
      <Layout style={{ minHeight: '100vh', backgroundColor: '#080d1a' }}>
        {/* Üst Bilgi ve Makro Metrikler */}
        <HeaderBar onNavigateTab={(tab) => handleNavigateTab(tab)} />

        {/* Ana İçerik ve Sekmeler */}
        <Content style={{ padding: '0 24px 24px 24px' }}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            type="card"
            size="large"
            items={[
              {
                key: 'chat',
                label: (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 8px' }}>
                    <MessageOutlined style={{ color: '#38bdf8' }} />
                    💬 Taktik Copilot
                  </span>
                ),
                children: (
                  <CopilotChat
                    onSelectTarget={(id, type) => handleOpenDetail(id, type)}
                    onNavigateTab={handleNavigateTab}
                  />
                )
              },
              {
                key: 'radar',
                label: (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 8px' }}>
                    <RadarChartOutlined style={{ color: '#38bdf8' }} />
                    📡 Radar (ELINT) Tablosu
                  </span>
                ),
                children: (
                  <RadarTable
                    onSelectEmission={(id) => handleOpenDetail(id, 'RADAR')}
                    initialFilter={radarFilter}
                  />
                )
              },
              {
                key: 'comint',
                label: (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 8px' }}>
                    <SecurityScanOutlined style={{ color: '#c084fc' }} />
                    📻 Muhabere (COMINT) Tablosu
                  </span>
                ),
                children: (
                  <ComintTable
                    onSelectComint={(id) => handleOpenDetail(id, 'COMINT')}
                    initialFilter={comintFilter}
                  />
                )
              }
            ]}
          />
        </Content>

        {/* 37 & 30 Parametreli Yan Panel (Drawer) */}
        <DetailDrawer
          visible={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          type={drawerType}
          data={drawerData}
          loading={drawerLoading}
          onSelectEmission={(id, type) => handleOpenDetail(id, type || drawerType)}
        />
      </Layout>
    </ConfigProvider>
  );
}
