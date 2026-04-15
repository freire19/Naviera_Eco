import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printBilhete, printListaPassageiros } from '../utils/print.js'
import ModalCriarPassagem from '../components/ModalCriarPassagem.jsx'
import ModalPagarPassagem from '../components/ModalPagarPassagem.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(val) {
  if (!val) return '\u2014'
  const d = new Date(val + 'T00:00:00')
  return d.toLocaleDateString('pt-BR')
}

export default function Passagens({ viagemAtiva }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)

  // Modal criar
  const [modalCriar, setModalCriar] = useState(false)

  // Modal pagar
  const [modalPagar, setModalPagar] = useState(null)

  // Toast
  const [toast, setToast] = useState(null)

  // Dados auxiliares para o modal
  const [rotas, setRotas] = useState([])
  const [acomodacoes, setAcomodacoes] = useState([])
  const [tiposPassagemAux, setTiposPassagemAux] = useState([])
  const [agentes, setAgentes] = useState([])
  const [nacionalidades, setNacionalidades] = useState([])
  const [tiposDocumento, setTiposDocumento] = useState([])
  const [sexos, setSexos] = useState([])
  const [caixas, setCaixas] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregarPassagens = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => showToast('Erro ao carregar passagens', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregarPassagens()
  }, [carregarPassagens])

  // Carregar dados auxiliares uma vez
  useEffect(() => {
    Promise.allSettled([
      api.get('/rotas').then(setRotas),
      api.get('/cadastros/acomodacoes').then(setAcomodacoes),
      api.get('/cadastros/tipos-passagem-aux').then(setTiposPassagemAux),
      api.get('/cadastros/agentes').then(setAgentes),
      api.get('/cadastros/nacionalidades').then(setNacionalidades),
      api.get('/cadastros/tipos-documento').then(setTiposDocumento),
      api.get('/cadastros/sexos').then(setSexos),
      api.get('/cadastros/caixas').then(setCaixas)
    ]).catch(() => {})
  }, [])

  // --- Excluir ---

  async function handleExcluir(passagem) {
    if (!window.confirm(`Excluir passagem ${passagem.numero_bilhete || passagem.id_passagem}?`)) return

    try {
      await api.delete(`/passagens/${passagem.id_passagem}`)
      showToast('Passagem excluida com sucesso')
      carregarPassagens()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir passagem', 'error')
    }
  }

  // --- Placeholder (sem viagem) ---

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83C\uDFAB'}</div>
        <h2>Passagens</h2>
        <p>Selecione uma viagem para ver as passagens.</p>
      </div>
    )
  }

  // --- Render ---

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.type}`}>{toast.msg}</div>
      )}

      <div className="card">
        <div className="card-header">
          <div className="toolbar">
            <h3>Passagens — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
            <button className="btn-primary" onClick={() => setModalCriar(true)}>
              + Nova Passagem
            </button>
            {passagens.length > 0 && (
              <button
                className="btn-sm primary"
                style={{ marginLeft: 8 }}
                onClick={() => printListaPassageiros(passagens, viagemAtiva)}
              >
                Imprimir Lista
              </button>
            )}
          </div>
          <span className="badge info">{passagens.length} registros</span>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Bilhete</th>
                  <th>Passageiro</th>
                  <th>Documento</th>
                  <th>Nascimento</th>
                  <th>Nacionalidade</th>
                  <th>Origem</th>
                  <th>Destino</th>
                  <th>Assento</th>
                  <th>Valor Total</th>
                  <th>Desconto</th>
                  <th>Pago</th>
                  <th>A Pagar</th>
                  <th>Devedor</th>
                  <th>Status</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {passagens.map(p => {
                  const restante = (p.valor_a_pagar || p.valor_total || 0) - (p.valor_pago || 0)
                  return (
                    <tr key={p.id_passagem}>
                      <td>{p.numero_bilhete}</td>
                      <td>{p.nome_passageiro || '\u2014'}</td>
                      <td>{p.numero_doc || '\u2014'}</td>
                      <td>{formatDate(p.data_nascimento)}</td>
                      <td>{p.nome_nacionalidade || '\u2014'}</td>
                      <td>{p.origem || '\u2014'}</td>
                      <td>{p.destino || '\u2014'}</td>
                      <td>{p.assento || '\u2014'}</td>
                      <td className="money">{formatMoney(p.valor_total)}</td>
                      <td className="money">{formatMoney(p.valor_desconto_geral)}</td>
                      <td className="money">{formatMoney(p.valor_pago)}</td>
                      <td className="money">{formatMoney(p.valor_a_pagar)}</td>
                      <td className="money">{formatMoney(p.valor_devedor)}</td>
                      <td>
                        <span className={`badge ${p.status_passagem === 'PAGO' ? 'success' : p.status_passagem === 'PARCIAL' ? 'warning' : 'danger'}`}>
                          {p.status_passagem || 'PENDENTE'}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn-sm primary"
                          onClick={() => printBilhete(p, viagemAtiva)}
                          style={{ marginRight: 6 }}
                        >
                          Imprimir
                        </button>
                        {restante > 0.01 && (
                          <button
                            className="btn-sm primary"
                            onClick={() => setModalPagar(p)}
                            style={{ marginRight: 6 }}
                          >
                            Pagar
                          </button>
                        )}
                        <button
                          className="btn-sm danger"
                          onClick={() => handleExcluir(p)}
                        >
                          Excluir
                        </button>
                      </td>
                    </tr>
                  )
                })}
                {passagens.length === 0 && (
                  <tr>
                    <td colSpan="15" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                      Nenhuma passagem nesta viagem
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Criar Passagem */}
      {modalCriar && (
        <ModalCriarPassagem
          viagemAtiva={viagemAtiva}
          rotas={rotas}
          acomodacoes={acomodacoes}
          tiposPassagemAux={tiposPassagemAux}
          agentes={agentes}
          nacionalidades={nacionalidades}
          tiposDocumento={tiposDocumento}
          sexos={sexos}
          caixas={caixas}
          onClose={() => setModalCriar(false)}
          onSuccess={() => { setModalCriar(false); carregarPassagens() }}
          showToast={showToast}
        />
      )}

      {/* Modal Pagar */}
      {modalPagar && (
        <ModalPagarPassagem
          passagem={modalPagar}
          caixas={caixas}
          onClose={() => setModalPagar(null)}
          onSuccess={() => { setModalPagar(null); carregarPassagens() }}
          showToast={showToast}
        />
      )}
    </div>
  )
}
