import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Card, Typography, Space, Tag, Collapse, Spin, Alert, Empty } from 'antd';
import {
  SendOutlined,
  ThunderboltOutlined,
  RadarChartOutlined,
  SecurityScanOutlined,
  CodeOutlined,
  RobotOutlined,
  UserOutlined,
  ArrowRightOutlined
} from '@ant-design/icons';
import axios from 'axios';
import { marked } from 'marked';

const { TextArea } = Input;
const { Text } = Typography;

export default function CopilotChat({ onSelectTarget, onNavigateTab }) {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: `### 🛡️ TAKTİK ELEKTRONİK HARP COPILOT SİSTEMİ DEVREDE
Sahadaki **Radar (ELINT)** ve **Muhabere (COMINT)** hedefleri operasyonel durumsal farkındalık için hazır.

Aşağıdaki hızlı komutları kullanabilir veya doğal dilde arama yapabilirsiniz:
- *'Düşman atış kontrol radarlarını ve HSS mevzilerini listele'*
- *'8500-10000 MHz aralığındaki X-band radarları getir'*
- *'Telsiz haberleşme ağını, aktif çağrı adı ikililerini ve temas adetlerini analiz et'*
- *'KARTAL-1 ve SHADOW-03 arasındaki telsiz ağını ve görüşme detaylarını çıkar'*
- *'Son 1 saat veya 24 saatteki telsiz trafiğini ve hareketlenmeleri analiz et'*
- *'Aktif elektronik taarruz uygulanan veya hudut tacizi yapan hedefler nelerdir?'*`,
      executionLogs: []
    }
  ]);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const quickPrompts = [
    { label: '📡 Genel Taktik Durumu Özetle', prompt: 'Sahadaki genel taktik durumu, düşman radarlarını ve telsiz muhabere resmini özetle.' },
    { label: '🎯 Düşman Radarları & HSS', prompt: 'Düşman hava savunma radarları ve atış kontrol mevzilerini listele.' },
    { label: '📻 Kriptolu Muhabere Yayınları', prompt: 'Sahadaki kriptolu telsiz yayınlarını ve yabancı lisan konuşmalarını listele.' },
    { label: '⚡ ET ve Hudut Tacizi Raporu', prompt: 'Aktif elektronik taarruz icra edilen veya hudut tacizi yapan hedefleri raporla.' },
    { label: '🔗 Telsiz Haberleşme Ağı Analizi', prompt: 'Sahadaki en aktif telsiz haberleşme ağlarını, çağrı adları arasındaki link ilişkilerini ve kripto durumunu analiz et.' },
    { label: '⏱️ Son 1 Saat Trafik & Hareketlenme', prompt: 'Son 1 saatteki telsiz trafiğini, en aktif konuşma çiftlerini ve ani bir hareketlenme/surge olup olmadığını analiz et.' }
  ];

  const handleSend = async (customPrompt) => {
    const textToSend = customPrompt || inputText;
    if (!textToSend || !textToSend.trim() || loading) return;

    const userMessage = {
      role: 'user',
      content: textToSend,
      time: new Date().toLocaleTimeString()
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputText('');
    setLoading(true);

    try {
      const res = await axios.post('/api/v1/tactical/analyze', {
        userPrompt: textToSend,
        conversationId: 'c2-tactical-session'
      });

      const assistantMessage = {
        role: 'assistant',
        content: res.data?.operationalBriefing || 'Yanıt alınamadı.',
        executionLogs: res.data?.executionLogs || [],
        time: new Date().toLocaleTimeString()
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      console.error('Sorgu işleme hatası:', err);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: '❌ **Taktik Analiz Hatası:** Sunucu bağlantısı sağlanamadı veya sorgu zaman aşımına uğradı.',
          executionLogs: ['Hata: ' + (err.message || 'Bilinmeyen hata')],
          isError: true
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  // Markdown içindeki [EMI-XXXX] ve [COM-XXXX] tıklanabilir yapmak için DOM tıklama delegasyonu
  const handleContentClick = (e) => {
    const target = e.target;
    if (target.dataset && target.dataset.targetId) {
      const id = target.dataset.targetId;
      const type = id.startsWith('EMI') ? 'RADAR' : 'COMINT';
      onSelectTarget(id, type);
    }
  };

  // Markdown HTML formatlama + Target Link Regex enjeksiyonu
  const formatMarkdown = (markdownText) => {
    if (!markdownText) return '';
    // [EMI-1001] -> clickable badge
    let processed = markdownText.replace(
      /\[(EMI-\d+)\]/g,
      `<span class="interactive-target-tag radar" data-target-id="$1" title="Radar Detayını Aç">$1</span>`
    );
    // [COM-2001] -> clickable badge
    processed = processed.replace(
      /\[(COM-\d+)\]/g,
      `<span class="interactive-target-tag comint" data-target-id="$1" title="Muhabere Detayını Aç">$1</span>`
    );

    return marked.parse(processed);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 190px)' }}>
      {/* Hızlı Komut Çipleri */}
      <div style={{ marginBottom: 12, overflowX: 'auto', whiteSpace: 'nowrap', paddingBottom: 4 }}>
        <Space size={8}>
          {quickPrompts.map((qp, idx) => (
            <Button
              key={idx}
              size="small"
              style={{
                backgroundColor: '#1e293b',
                borderColor: '#334155',
                color: '#cbd5e1',
                borderRadius: 16,
                fontSize: 12
              }}
              onClick={() => handleSend(qp.prompt)}
              disabled={loading}
            >
              {qp.label}
            </Button>
          ))}
        </Space>
      </div>

      {/* Mesaj Listesi */}
      <Card
        style={{
          flex: 1,
          overflowY: 'auto',
          backgroundColor: '#0f172a',
          borderColor: '#1e293b',
          borderRadius: 8,
          marginBottom: 12
        }}
        bodyStyle={{ padding: 16 }}
        onClick={handleContentClick}
      >
        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          return (
            <div
              key={index}
              style={{
                display: 'flex',
                justifyContent: isUser ? 'flex-end' : 'flex-start',
                marginBottom: 16
              }}
            >
              <div
                style={{
                  maxWidth: isUser ? '75%' : '90%',
                  display: 'flex',
                  gap: 12,
                  flexDirection: isUser ? 'row-reverse' : 'row'
                }}
              >
                {/* Avatar */}
                <div
                  style={{
                    width: 34,
                    height: 34,
                    borderRadius: '50%',
                    backgroundColor: isUser ? '#0284c7' : '#0369a1',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    flexShrink: 0
                  }}
                >
                  {isUser ? <UserOutlined /> : <RobotOutlined />}
                </div>

                {/* Bubble */}
                <div
                  style={{
                    backgroundColor: isUser ? '#1e293b' : '#111827',
                    border: `1px solid ${isUser ? '#334155' : '#1f2937'}`,
                    borderRadius: 8,
                    padding: '12px 16px',
                    color: '#e2e8f0',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                  }}
                >
                  {isUser ? (
                    <Text style={{ color: '#f1f5f9', whiteSpace: 'pre-wrap' }}>{msg.content}</Text>
                  ) : (
                    <div>
                      <div
                        className="tactical-markdown"
                        dangerouslySetInnerHTML={{ __html: formatMarkdown(msg.content) }}
                      />

                      {/* Tabloya Yönlendirme Aksiyon Butonları */}
                      <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        <Button
                          size="small"
                          type="primary"
                          ghost
                          icon={<RadarChartOutlined />}
                          onClick={() => onNavigateTab('radar')}
                          style={{ borderColor: '#38bdf8', color: '#38bdf8' }}
                        >
                          📡 Radar Tablosunda İncele
                        </Button>
                        <Button
                          size="small"
                          type="primary"
                          ghost
                          icon={<SecurityScanOutlined />}
                          onClick={() => onNavigateTab('comint')}
                          style={{ borderColor: '#c084fc', color: '#c084fc' }}
                        >
                          📻 Muhabere Tablosunda İncele
                        </Button>
                      </div>

                      {/* LLM Telemetri Logları */}
                      {msg.executionLogs && msg.executionLogs.length > 0 && (
                        <Collapse
                          ghost
                          size="small"
                          style={{ marginTop: 12, borderTop: '1px solid #1f2937' }}
                          items={[
                            {
                              key: '1',
                              label: (
                                <Text type="secondary" style={{ fontSize: 11 }}>
                                  <CodeOutlined /> Telemetri Araç Çağrıları ({msg.executionLogs.length})
                                </Text>
                              ),
                              children: (
                                <div style={{
                                  backgroundColor: '#090d16',
                                  padding: '8px 12px',
                                  borderRadius: 4,
                                  fontFamily: 'JetBrains Mono, monospace',
                                  fontSize: 11,
                                  color: '#38bdf8'
                                }}>
                                  {msg.executionLogs.map((log, i) => (
                                    <div key={i} style={{ marginBottom: 4 }}>• {log}</div>
                                  ))}
                                </div>
                              )
                            }
                          ]}
                        />
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {loading && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '16px 0' }}>
            <div style={{
              width: 34,
              height: 34,
              borderRadius: '50%',
              backgroundColor: '#0369a1',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff'
            }}>
              <RobotOutlined />
            </div>
            <div style={{
              backgroundColor: '#111827',
              border: '1px solid #1f2937',
              borderRadius: 8,
              padding: '10px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: 12
            }}>
              <Spin size="small" />
              <Text type="secondary">Taktik Copilot veritabanı analizini işliyor...</Text>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </Card>

      {/* Giriş Çubuğu */}
      <div style={{ display: 'flex', gap: 8 }}>
        <TextArea
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Komutanım, sahadaki radar emisyonları, telsiz muhabere ağları veya elektronik taarruz hakkında bir soru yazınız..."
          autoSize={{ minRows: 2, maxRows: 4 }}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          disabled={loading}
          style={{
            backgroundColor: '#0f172a',
            borderColor: '#334155',
            color: '#f8fafc',
            borderRadius: 8
          }}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={() => handleSend()}
          loading={loading}
          style={{
            height: 'auto',
            padding: '0 24px',
            backgroundColor: '#0284c7',
            borderColor: '#0284c7',
            borderRadius: 8
          }}
        >
          Gönder
        </Button>
      </div>
    </div>
  );
}


