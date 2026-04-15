import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { exportCSV } from '../utils/export.js'
import { formatMoney } from '../components/financeiro/formatters.js'
import TabResumo from '../components/financeiro/TabResumo.jsx'
import TabDetalhe from '../components/financeiro/TabDetalhe.jsx'
import TabSaidas from '../components/financeiro/TabSaidas.jsx'

const TABS = ['Resumo', 'Passagens', 'Encomendas', 'Fretes', 'Saidas']

const COL_PASSAGENS = [
  { key: 'numero_bilhete', label: 'Bilhete' },
  { key: 'nome_passageiro', label: 'Passageiro' },
  { key: 'valor_total', label: 'Valor Total', format: 'money' },
  { key: 'valor_pago', label: 'Valor Pago', format: 'money' },
  { key: 'valor_devedor', label: 'Devedor', format: 'money' },
  { key: 'status_passagem', label: 'Status', badge: r => ({ className: r.status_passagem === 'PAGO' ? 'success' : 'warning', label: r.status_passagem || 'Pendente' }) },
  { key: 'data_emissao', label: 'Data', format: 'date' }
]

const COL_ENCOMENDAS = [
  { key: 'numero_encomenda', label: 'Numero' },
  { key: 'remetente', label: 'Remetente' },
  { key: 'destinatario', label: 'Destinatario' },
  { key: 'total_a_pagar', label: 'Total a Pagar', format: 'money' },
  { key: 'valor_pago', label: 'Pago', format: 'money' },
  { key: 'status_pagamento', label: 'Pagamento', badge: r => ({ className: r.status_pagamento === 'PAGO' ? 'success' : 'warning', label: r.status_pagamento || 'Pendente' }) },
  { key: 'entregue', label: 'Entrega', badge: r => ({ className: r.entregue ? 'success' : 'warning', label: r.entregue ? 'Sim' : 'Nao' }) },
  { key: 'data_emissao', label: 'Data', format: 'date' }
]

const COL_FRETES = [
  { key: 'numero_frete', label: 'Numero' },
  { key: 'remetente_nome_temp', label: 'Remetente' },
  { key: 'destinatario_nome_temp', label: 'Destinatario' },
  { key: 'valor_nominal', label: 'Valor Nominal', format: 'money' },
  { key: 'valor_pago', label: 'Valor Pago', format: 'money' },
  { key: 'status', label: 'Status', badge: r => ({ className: r.status === 'PAGO' ? 'success' : 'warning', label: r.status || 'Pendente' }) },
  { key: 'data_emissao', label: 'Data', format: 'date' }
]

const CSV_PASSAGENS = [
  { key: 'numero_bilhete', label: 'Bilhete' },
  { key: 'nome_passageiro', label: 'Passageiro' },
  { key: 'valor_total', label: 'Valor Total' },
  { key: 'valor_pago', label: 'Valor Pago' },
  { key: 'valor_devedor', label: 'Valor Devedor' },
  { key: 'status_passagem', label: 'Status' },
  { key: 'data_emissao', label: 'Data Emissao' }
]

const CSV_ENCOMENDAS = [
  { key: 'numero_encomenda', label: 'Numero' },
  { key: 'remetente', label: 'Remetente' },
  { key: 'destinatario', label: 'Destinatario' },
  { key: 'total_a_pagar', label: 'Total a Pagar' },
  { key: 'valor_pago', label: 'Valor Pago' },
  { key: 'status_pagamento', label: 'Pagamento' },
  { key: 'entregue', label: 'Entregue' },
  { key: 'data_emissao', label: 'Data Emissao' }
]

const CSV_FRETES = [
  { key: 'numero_frete', label: 'Numero' },
  { key: 'remetente_nome_temp', label: 'Remetente' },
  { key: 'destinatario_nome_temp', label: 'Destinatario' },
  { key: 'valor_nominal', label: 'Valor Nominal' },
  { key: 'valor_pago', label: 'Valor Pago' },
  { key: 'status', label: 'Status' },
  { key: 'data_emissao', label: 'Data Emissao' }
]

const CSV_SAIDAS = [
  { key: 'descricao', label: 'Descricao' },
  { key: 'valor_total', label: 'Valor' },
  { key: 'tipo', label: 'Tipo' },
  { key: 'data_vencimento', label: 'Data' },
  { key: 'observacoes', label: 'Observacoes' }
]

function sumField(arr, field) {
  return arr.reduce((s, r) => s + (parseFloat(r[field]) || 0), 0)
}

export default function Financeiro({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  const [saidas, setSaidas] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingSaidas, setLoadingSaidas] = useState(false)
  const [toast, setToast] = useState(null)
  const [tabAtiva, setTabAtiva] = useState('Resumo')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [detPassagens, setDetPassagens] = useState([])
  const [detEncomendas, setDetEncomendas] = useState([])
  const [detFretes, setDetFretes] = useState([])
  const [loadingDetail, setLoadingDetail] = useState(false)

  const mostrarToast = useCallback((msg, tipo = 'success') => {
    setToast({ msg, tipo })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const carregarBalanco = useCallback(() => {
    if (!viagemAtiva) return
    api.get(`/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setBalanco)
      .catch(() => {})
  }, [viagemAtiva])

  const carregarSaidas = useCallback(() => {
    if (!viagemAtiva) return
    setLoadingSaidas(true)
    api.get(`/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(data => setSaidas(Array.isArray(data) ? data : []))
      .catch(() => setSaidas([]))
      .finally(() => setLoadingSaidas(false))
  }, [viagemAtiva])

  const carregarDetalhes = useCallback(() => {
    if (!viagemAtiva) return
    setLoadingDetail(true)
    const params = `viagem_id=${viagemAtiva.id_viagem}${dataInicio ? `&data_inicio=${dataInicio}` : ''}${dataFim ? `&data_fim=${dataFim}` : ''}`
    Promise.all([
      api.get(`/financeiro/passagens?${params}`).catch(() => []),
      api.get(`/financeiro/encomendas?${params}`).catch(() => []),
      api.get(`/financeiro/fretes?${params}`).catch(() => [])
    ]).then(([p, e, f]) => {
      setDetPassagens(Array.isArray(p) ? p : [])
      setDetEncomendas(Array.isArray(e) ? e : [])
      setDetFretes(Array.isArray(f) ? f : [])
    }).finally(() => setLoadingDetail(false))
  }, [viagemAtiva, dataInicio, dataFim])

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    Promise.all([
      api.get(`/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`).then(setBalanco).catch(() => {}),
      api.get(`/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`).then(data => setSaidas(Array.isArray(data) ? data : [])).catch(() => setSaidas([]))
    ]).finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    if (['Passagens', 'Encomendas', 'Fretes'].includes(tabAtiva)) {
      carregarDetalhes()
    }
  }, [tabAtiva, carregarDetalhes])

  function handleDadosAlterados() {
    carregarSaidas()
    carregarBalanco()
  }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCB0'}</div>
        <h2>Financeiro</h2>
        <p>Selecione uma viagem para ver o financeiro.</p>
      </div>
    )
  }

  const vid = viagemAtiva.id_viagem

  return (
    <div>
      {loading && <p style={{ color: 'var(--text-muted)' }}>Carregando...</p>}

      {toast && (
        <div className={`toast ${toast.tipo}`}>
          {toast.msg}
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: '1rem', flexWrap: 'wrap' }}>
        {TABS.map(tab => (
          <button
            key={tab}
            onClick={() => setTabAtiva(tab)}
            className={tabAtiva === tab ? 'btn-primary' : 'btn-secondary'}
            style={{ padding: '6px 16px', fontSize: 13 }}
          >
            {tab}
          </button>
        ))}
      </div>

      {tabAtiva === 'Resumo' && <TabResumo balanco={balanco} />}

      {tabAtiva === 'Passagens' && (
        <TabDetalhe
          titulo="Passagens Detalhado"
          dados={detPassagens}
          colunas={COL_PASSAGENS}
          idKey="id_passagem"
          loading={loadingDetail}
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onExportar={() => exportCSV(detPassagens, CSV_PASSAGENS, `passagens_viagem_${vid}`)}
          sumarios={detPassagens.length > 0 ? [
            { label: 'Total', value: detPassagens.length },
            { label: 'Valor Total', className: 'money', render: () => formatMoney(sumField(detPassagens, 'valor_total')) },
            { label: 'Pago', className: 'money positive', render: () => formatMoney(sumField(detPassagens, 'valor_pago')) }
          ] : null}
        />
      )}

      {tabAtiva === 'Encomendas' && (
        <TabDetalhe
          titulo="Encomendas Detalhado"
          dados={detEncomendas}
          colunas={COL_ENCOMENDAS}
          idKey="id_encomenda"
          loading={loadingDetail}
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onExportar={() => exportCSV(detEncomendas, CSV_ENCOMENDAS, `encomendas_viagem_${vid}`)}
          sumarios={detEncomendas.length > 0 ? [
            { label: 'Total', value: detEncomendas.length },
            { label: 'A Pagar', className: 'money', render: () => formatMoney(sumField(detEncomendas, 'total_a_pagar')) },
            { label: 'Pago', className: 'money positive', render: () => formatMoney(sumField(detEncomendas, 'valor_pago')) }
          ] : null}
        />
      )}

      {tabAtiva === 'Fretes' && (
        <TabDetalhe
          titulo="Fretes Detalhado"
          dados={detFretes}
          colunas={COL_FRETES}
          idKey="id_frete"
          loading={loadingDetail}
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onExportar={() => exportCSV(detFretes, CSV_FRETES, `fretes_viagem_${vid}`)}
          sumarios={detFretes.length > 0 ? [
            { label: 'Total', value: detFretes.length },
            { label: 'Valor Nominal', className: 'money', render: () => formatMoney(sumField(detFretes, 'valor_nominal')) },
            { label: 'Pago', className: 'money positive', render: () => formatMoney(sumField(detFretes, 'valor_pago')) }
          ] : null}
        />
      )}

      {tabAtiva === 'Saidas' && (
        <TabSaidas
          saidas={saidas}
          loadingSaidas={loadingSaidas}
          filtroTipo={filtroTipo}
          onFiltroTipoChange={setFiltroTipo}
          onExportar={(filtered) => exportCSV(filtered, CSV_SAIDAS, `saidas_viagem_${vid}`)}
          viagemAtiva={viagemAtiva}
          mostrarToast={mostrarToast}
          onDadosAlterados={handleDadosAlterados}
        />
      )}
    </div>
  )
}
