import React, { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Tag,
  Button,
  Select,
  Typography,
  Spin,
  Alert,
  Table,
  Badge,
  Space,
  Tooltip,
  Divider,
  Statistic
} from 'antd';
import {
  AimOutlined,
  ReloadOutlined,
  WarningOutlined,
  RadarChartOutlined,
  ThunderboltOutlined,
  SecurityScanOutlined,
  FileProtectOutlined,
  CheckCircleOutlined,
  AuditOutlined
} from '@ant-design/icons';
import axios from 'axios';

const { Title, Text, Paragraph } = Typography;

export default function ThreatAnalysisDashboard({ onSelectEmission }) {
  const [sector, setSector] = useState('Ege');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchThreatAnalysis = async (selectedSector) => {
    setLoading(true);
    try {
      const url = selectedSector && selectedSector !== 'TÜMÜ'
        ? `/api/v1/tactical/analysis/threat-assessment?bolge=${encodeURIComponent(selectedSector)}`
        : `/api/v1/tactical/analysis/threat-assessment`;
      const res = await axios.get(url);
      setData(res.data);
    } catch (err) {
      console.error('Tehdit analizi yüklenemedi:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchThreatAnalysis(sector);
  }, [sector]);

  // Tehdit Seviyesi Renk ve İkon Yardımcısı
  const getSeverityMeta = (level) => {
    switch (level) {
      case 'SEVIYE_4_KRITIK':
        return { color: '#ef4444', label: 'SEVİYE 4 - KRİTİK ANGAJMAN', badgeStatus: 'error' };
      case 'SEVIYE_3_YUKSEK':
        return { color: '#f97316', label: 'SEVİYE 3 - HEDEF TAKİP & HEDEFLEME', badgeStatus: 'warning' };
      case 'SEVIYE_2_ORTA':
        return { color: '#eab308', label: 'SEVİYE 2 - ERKEN UYARI & TEŞHİS', badgeStatus: 'processing' };
      default:
        return { color: '#22c55e', label: 'SEVİYE 1 - RUTİN GÖZETLEME', badgeStatus: 'success' };
    }
  };

  const getKillChainTag = (stage) => {
    switch (stage) {
      case 'ENGAGE':
        return <Tag color="error" icon={<AimOutlined />}>ENGAGE (Ateş/Güdüm)</Tag>;
      case 'TARGET':
        return <Tag color="volcano" icon={<ThunderboltOutlined />}>TARGET (Atış Kontrol)</Tag>;
      case 'TRACK':
        return <Tag color="warning" icon={<RadarChartOutlined />}>TRACK (Hassas Takip)</Tag>;
      case 'FIX':
        return <Tag color="gold" icon={<SecurityScanOutlined />}>FIX (Teşhis/Konum)</Tag>;
      default:
        return <Tag color="green" icon={<CheckCircleOutlined />}>FIND (Erken İhbar)</Tag>;
    }
  };

  const columns = [
    {
      title: 'Tehdit / Kill-Chain',
      key: 'threat',
      width: 170,
      render: (_, record) => {
        const meta = getSeverityMeta(record.threatLevel);
        return (
          <Space direction="vertical" size={4}>
            <Tag color={meta.color} style={{ fontWeight: 'bold' }}>
              {record.threatLevel === 'SEVIYE_4_KRITIK' ? '🔴 KRİTİK' :
               record.threatLevel === 'SEVIYE_3_YUKSEK' ? '🟠 YÜKSEK' :
               record.threatLevel === 'SEVIYE_2_ORTA' ? '🟡 ORTA' : '🟢 DÜŞÜK'}
            </Tag>
            {getKillChainTag(record.killChainStage)}
            <Text type="secondary" style={{ fontSize: 11 }}>Skor: {record.score}/100</Text>
          </Space>
        );
      }
    },
    {
      title: 'Radar & Hava Savunma (HSS)',
      key: 'radarInfo',
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Text strong style={{ color: '#38bdf8', fontSize: 14 }}>{record.radarAdi}</Text>
          <Text style={{ fontSize: 12, color: '#e2e8f0' }}>HSS: <b>{record.hss || 'Stand-alone'}</b></Text>
          <Space size={4}>
            <Tag color="geekblue" style={{ fontSize: 10 }}>{record.emissionId}</Tag>
            <Tag color="cyan" style={{ fontSize: 10 }}>{record.elintNotasyonu}</Tag>
            <Tag color={record.teshisKimlik === 'DUSMAN' ? 'red' : 'orange'} style={{ fontSize: 10 }}>
              {record.teshisKimlik}
            </Tag>
          </Space>
        </Space>
      )
    },
    {
      title: 'Mevzi / Bölge Bilgisi',
      key: 'location',
      width: 200,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Text strong style={{ color: '#f8fafc' }}>{record.hedefYerBilgisi}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.hedefMevziBilgisi || 'Mevzi Tanımsız'}</Text>
          {record.taciz && (
            <Tag color="error" style={{ animation: 'pulse 1.5s infinite', fontWeight: 'bold' }}>
              ⚠️ KİLİTLENME (TACİZ) AKTİF
            </Tag>
          )}
        </Space>
      )
    },
    {
      title: 'Dalga Formu & Skolnik Çözünürlüğü',
      key: 'physics',
      width: 230,
      render: (_, record) => (
        <Space direction="vertical" size={2} style={{ fontSize: 12 }}>
          <div><Text type="secondary">Frekans:</Text> <Text code>{record.minFrekansMhz ? `${record.minFrekansMhz.toFixed(0)} MHz` : '-'}</Text></div>
          <div>
            <Text type="secondary">PW:</Text> <Text code>{record.minPwMicroSec ? `${record.minPwMicroSec.toFixed(2)} µs` : '-'}</Text>
            {record.skolnikRangeResolutionMeters && (
              <Tooltip title="Skolnik Menzil Çözünürlüğü: ΔR = (c * τ) / 2">
                <Tag color="purple" style={{ marginLeft: 6, fontSize: 10 }}>
                  Çöz: {record.skolnikRangeResolutionMeters.toFixed(0)}m
                </Tag>
              </Tooltip>
            )}
          </div>
          <div>
            <Text type="secondary">PRI / PRF:</Text> <Text code>{record.minPriMicroSec ? `${record.minPriMicroSec.toFixed(0)} µs` : '-'}</Text>
            {record.prfHz && (
              <Tag color="blue" style={{ marginLeft: 6, fontSize: 10 }}>
                {record.prfHz.toFixed(0)} Hz
              </Tag>
            )}
          </div>
          <div><Text type="secondary">Genlik:</Text> <Text style={{ color: '#fbbf24' }}>{record.genlikDbm ? `${record.genlikDbm.toFixed(1)} dBm` : '-'}</Text></div>
        </Space>
      )
    },
    {
      title: 'Fiziksel Gerekçe & Önerilen Karşı Tedbir (ET)',
      key: 'action',
      render: (_, record) => (
        <Space direction="vertical" size={4}>
          <div style={{ backgroundColor: '#1e293b', padding: '6px 10px', borderRadius: 4, borderLeft: '3px solid #38bdf8' }}>
            <Text style={{ fontSize: 12, color: '#cbd5e1' }}>
              <b>İspat:</b> {record.physicalRationale}
            </Text>
            <div style={{ marginTop: 2 }}>
              <Text type="secondary" style={{ fontSize: 10, fontStyle: 'italic' }}>
                Ref: {record.doctrinalReference}
              </Text>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4, flexWrap: 'wrap' }}>
            <Tag color="magenta" icon={<ThunderboltOutlined />} style={{ fontWeight: 'bold' }}>
              {record.recommendedEWAction}
            </Tag>
            <Tag color="red">{record.timeCriticality}</Tag>
            <Button
              type="primary"
              size="small"
              icon={<AuditOutlined />}
              onClick={() => onSelectEmission(record.emissionId, 'RADAR')}
              style={{ fontSize: 11, height: 24 }}
            >
              37 Parametreli Kartı Aç
            </Button>
          </div>
        </Space>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Sektör Kontrol Bandı */}
      <Card
        size="small"
        style={{
          backgroundColor: '#0f172a',
          borderColor: '#1e293b',
          borderRadius: 8
        }}
      >
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col>
            <Space size={12} align="center">
              <AimOutlined style={{ fontSize: 24, color: '#ef4444' }} />
              <div>
                <Title level={4} style={{ margin: 0, color: '#f8fafc' }}>
                  Harekât Karar Destek & Düşman Niyet Analizi
                </Title>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  NATO F2T2EA Kill-Chain, Skolnik Menzil Çözünürlüğü ve Wiley ELINT Sinyal İşleme Motoru
                </Text>
              </div>
            </Space>
          </Col>
          <Col>
            <Space size={12}>
              <Text style={{ color: '#cbd5e1' }}>Harekât Sektörü:</Text>
              <Select
                value={sector}
                onChange={setSector}
                style={{ width: 220 }}
                options={[
                  { value: 'TÜMÜ', label: '🌐 Tüm Harekât Sahası' },
                  { value: 'Ege', label: '🌊 Ege Denizi / Adalar' },
                  { value: 'Midilli', label: '📍 Midilli Sektörü' },
                  { value: 'Sakız', label: '📍 Sakız Sektörü' },
                  { value: 'Akdeniz', label: '⚓ Doğu Akdeniz / Kıbrıs' },
                  { value: 'Karadeniz', label: '🚢 Karadeniz Sektörü' },
                  { value: 'Güneydoğu', label: '🛡️ Güneydoğu / Şırnak' },
                  { value: 'Suriye', label: '⚔️ Suriye / Tel Rıfat' }
                ]}
              />
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                loading={loading}
                onClick={() => fetchThreatAnalysis(sector)}
              >
                Yenile
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {loading && !data ? (
        <Card style={{ textAlign: 'center', padding: 60, backgroundColor: '#0f172a', borderColor: '#1e293b' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: '#94a3b8' }}>
            Harekât sahasındaki radar ve haberleşme verileri analiz ediliyor, Kill-Chain ve niyet çıkarımı yapılıyor...
          </div>
        </Card>
      ) : data ? (
        <>
          {/* Taktik Brifing & Executive Summary Kartı */}
          <Card
            style={{
              backgroundColor: '#0f172a',
              borderColor: data.genelTehditSeviyesi === 'SEVIYE_4_KRITIK' ? '#ef4444' : '#f97316',
              boxShadow: data.genelTehditSeviyesi === 'SEVIYE_4_KRITIK' ? '0 0 15px rgba(239, 68, 68, 0.25)' : 'none',
              borderRadius: 8
            }}
          >
            <Row gutter={[24, 16]}>
              <Col xs={24} lg={16}>
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                    <Tag
                      color={data.genelTehditSeviyesi === 'SEVIYE_4_KRITIK' ? '#ef4444' : '#f97316'}
                      style={{ fontSize: 13, padding: '4px 10px', fontWeight: 'bold' }}
                    >
                      {data.genelTehditSeviyesi === 'SEVIYE_4_KRITIK' ? '🔴 FLAŞ HAREKÂT TEHDİDİ' : '🟠 YÜKSEK TEHDİT DURUMU'}
                    </Tag>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Analiz Zamanı: {data.analizZamaniFormatted}
                    </Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Sektör: <b>{data.bolge}</b>
                    </Text>
                  </div>

                  <div style={{ backgroundColor: '#1e293b', padding: 14, borderRadius: 6, borderLeft: '4px solid #ef4444', marginTop: 4 }}>
                    <Title level={5} style={{ margin: '0 0 6px 0', color: '#f8fafc' }}>
                      📋 Harekât Kurmayı Taktik Durum Değerlendirmesi:
                    </Title>
                    <Paragraph style={{ margin: 0, color: '#e2e8f0', fontSize: 14, lineHeight: '1.6' }}>
                      {data.genelHarekatOzeti}
                    </Paragraph>
                  </div>

                  {data.comintKorelasyonNotu && (
                    <div style={{ backgroundColor: '#131b2e', padding: '10px 12px', borderRadius: 6, borderLeft: '4px solid #c084fc', marginTop: 4 }}>
                      <Text style={{ color: '#c084fc', fontWeight: 'bold' }}>
                        📻 Telsiz (COMINT) Çapraz Korelasyonu:
                      </Text>
                      <div style={{ color: '#cbd5e1', fontSize: 12, marginTop: 4 }}>
                        {data.comintKorelasyonNotu}
                      </div>
                    </div>
                  )}
                </Space>
              </Col>

              <Col xs={24} lg={8}>
                <Card
                  size="small"
                  style={{ backgroundColor: '#1e293b', borderColor: '#334155', height: '100%' }}
                >
                  <Title level={5} style={{ margin: '0 0 12px 0', color: '#f8fafc' }}>
                    Sektör Tehdit Dağılımı
                  </Title>
                  <Row gutter={[12, 12]}>
                    <Col span={12}>
                      <Statistic
                        title={<span style={{ color: '#ef4444' }}>🔴 Kritik (Engage)</span>}
                        value={data.kritikTehditSayisi}
                        valueStyle={{ color: '#ef4444', fontWeight: 'bold' }}
                      />
                    </Col>
                    <Col span={12}>
                      <Statistic
                        title={<span style={{ color: '#f97316' }}>🟠 Takip (Target/Track)</span>}
                        value={data.yuksekTehditSayisi}
                        valueStyle={{ color: '#f97316', fontWeight: 'bold' }}
                      />
                    </Col>
                    <Col span={12}>
                      <Statistic
                        title={<span style={{ color: '#eab308' }}>🟡 Erken Uyarı (Fix)</span>}
                        value={data.ortaTehditSayisi}
                        valueStyle={{ color: '#eab308', fontWeight: 'bold' }}
                      />
                    </Col>
                    <Col span={12}>
                      <Statistic
                        title={<span style={{ color: '#94a3b8' }}>📡 Taranan Emisyon</span>}
                        value={data.toplamTarananYayin}
                        valueStyle={{ color: '#f8fafc', fontWeight: 'bold' }}
                      />
                    </Col>
                  </Row>
                </Card>
              </Col>
            </Row>
          </Card>

          {/* Tehditler ve Kill-Chain Analiz Tablosu */}
          <Card
            title={
              <Space>
                <AimOutlined style={{ color: '#ef4444' }} />
                <span>Öncelikli Hedef ve Tehdit Sınıflandırma Listesi ({data.tehditler ? data.tehditler.length : 0})</span>
              </Space>
            }
            size="small"
            style={{ backgroundColor: '#0f172a', borderColor: '#1e293b', borderRadius: 8 }}
          >
            <Table
              dataSource={data.tehditler || []}
              columns={columns}
              rowKey="emissionId"
              pagination={{ pageSize: 8, showSizeChanger: true }}
              size="middle"
              scroll={{ x: 1000 }}
            />
          </Card>
        </>
      ) : (
        <Alert
          message="Veri Bulunamadı"
          description="Seçilen sektör için taktik radar yayını veya tehdit kaydı bulunamadı."
          type="info"
          showIcon
        />
      )}
    </div>
  );
}
