package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Modelo unificado de Passagem — 71 campos em uma unica classe.
 *
 * <p>Esta classe mistura campos de persistencia (colunas da tabela {@code passagens}),
 * campos de pagamento (valores mistos dinheiro/cartao/pix) e campos de exibicao
 * (preenchidos por JOINs nos DAOs para uso direto em TableView via PropertyValueFactory).
 *
 * <p><b>Por que nao foi dividida em sub-classes:</b>
 * <ul>
 *   <li>Todos os DAOs (PassagemDAO, VenderPassagemController, relatorios) montam o objeto
 *       inteiro em um unico ResultSet com JOINs — separar exigiria reescrever cada query.</li>
 *   <li>JavaFX {@code PropertyValueFactory} espera getters diretamente na classe do item
 *       da TableView — composicao (ex: {@code passagem.getPassageiro().getNome()}) nao funciona
 *       sem CellValueFactory customizada em cada coluna.</li>
 *   <li>O projeto nao tem testes automatizados suficientes para garantir uma refatoracao segura.</li>
 * </ul>
 *
 * <p><b>Organizacao dos campos:</b> os campos estao agrupados em secoes logicas
 * (Identificacao, Passageiro, Viagem/Rota, Valores, Pagamento, Status, Display)
 * para facilitar a navegacao. Campos marcados como "display-only (JOIN)" nao sao
 * persistidos diretamente na tabela {@code passagens} — sao preenchidos por JOINs.
 *
 * @see dao.PassagemDAO
 */
public class Passagem {

    // ====== Identificacao ======
    private Long id;
    private Integer numBilhete;

    // ====== Chaves estrangeiras (FKs) ======
    private Long idPassageiro;
    private Long idViagem;
    private Long idRota;
    private Integer idAcomodacao;
    private Integer idTipoPassagem;
    private Integer idAgente;

    /**
     * @deprecated Mantido apenas para compatibilidade com codigo legado e leitura de registros antigos.
     *             Pagamentos agora usam os campos {@code valorPagamentoDinheiro}, {@code valorPagamentoCartao}
     *             e {@code valorPagamentoPix}. Nao usar em logica nova.
     */
    @Deprecated
    private Integer idFormaPagamento;

    private Integer idCaixa;
    private Integer idUsuarioEmissor;
    private Integer idHorarioSaida;

    // ====== Emissao / Assento ======
    private LocalDate dataEmissao;
    private String assento;
    private String requisicao;
    private Integer ordem;

    // ====== Valores financeiros ======
    private BigDecimal valorAlimentacao;
    private BigDecimal valorTransporte;
    private BigDecimal valorCargas;
    private BigDecimal valorDescontoTarifa;
    private BigDecimal valorTotal;
    private BigDecimal valorDesconto;
    private BigDecimal valorAPagar;

    // ====== Pagamento (misto: dinheiro + cartao + pix) ======
    private BigDecimal valorPago;
    private BigDecimal valorPagamentoDinheiro = BigDecimal.ZERO;
    private BigDecimal valorPagamentoCartao = BigDecimal.ZERO;
    private BigDecimal valorPagamentoPix = BigDecimal.ZERO;
    private BigDecimal troco;
    private BigDecimal devedor;

    // ====== Status / Controle ======
    private String statusPassagem;
    private String observacoes;

    // ====== Display: Passageiro (preenchidos por JOINs) ======
    private String nomePassageiro;       // display-only (JOIN)
    private String numeroDoc;            // display-only (JOIN)
    private LocalDate dataNascimento;    // display-only (JOIN)
    private String sexo;                 // display-only (JOIN)
    private String tipoDoc;              // display-only (JOIN)
    private String nacionalidade;        // display-only (JOIN)
    private int idade;                   // display-only (JOIN) — calculado

    // ====== Display: Viagem / Rota (preenchidos por JOINs) ======
    private LocalDate dataViagem;              // display-only (JOIN)
    private String descricaoHorarioSaida;      // display-only (JOIN)
    private String origem;                     // display-only (JOIN)
    private String destino;                    // display-only (JOIN)
    private String strViagem;                  // display-only (JOIN)

    // ====== Display: Auxiliares (preenchidos por JOINs) ======
    private String acomodacao;           // display-only (JOIN)
    private String tipoPassagemAux;      // display-only (JOIN)
    private String agenteAux;            // display-only (JOIN)
    private String formaPagamento;       // display-only (JOIN) ou calculado pelo getter inteligente
    private String caixa;                // display-only (JOIN)

    public Passagem() {}

    // ====== Getters e Setters: Identificacao ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNumBilhete() { return numBilhete; }
    public void setNumBilhete(Integer numBilhete) { this.numBilhete = numBilhete; }

    // ====== Getters e Setters: Chaves estrangeiras ======

    public Long getIdPassageiro() { return idPassageiro; }
    public void setIdPassageiro(Long idPassageiro) { this.idPassageiro = idPassageiro; }

    public Long getIdViagem() { return idViagem; }
    public void setIdViagem(Long idViagem) { this.idViagem = idViagem; }

    public Long getIdRota() { return idRota; }
    public void setIdRota(Long idRota) { this.idRota = idRota; }

    public Integer getIdAcomodacao() { return idAcomodacao; }
    public void setIdAcomodacao(Integer idAcomodacao) { this.idAcomodacao = idAcomodacao; }

    public Integer getIdTipoPassagem() { return idTipoPassagem; }
    public void setIdTipoPassagem(Integer idTipoPassagem) { this.idTipoPassagem = idTipoPassagem; }

    public Integer getIdAgente() { return idAgente; }
    public void setIdAgente(Integer idAgente) { this.idAgente = idAgente; }

    /**
     * @deprecated Use os campos {@code valorPagamentoDinheiro/Cartao/Pix} em vez de idFormaPagamento.
     */
    @Deprecated
    public Integer getIdFormaPagamento() { return idFormaPagamento; }
    /**
     * @deprecated Use os campos {@code valorPagamentoDinheiro/Cartao/Pix} em vez de idFormaPagamento.
     */
    @Deprecated
    public void setIdFormaPagamento(Integer idFormaPagamento) { this.idFormaPagamento = idFormaPagamento; }

    public Integer getIdCaixa() { return idCaixa; }
    public void setIdCaixa(Integer idCaixa) { this.idCaixa = idCaixa; }

    public Integer getIdUsuarioEmissor() { return idUsuarioEmissor; }
    public void setIdUsuarioEmissor(Integer idUsuarioEmissor) { this.idUsuarioEmissor = idUsuarioEmissor; }

    public Integer getIdHorarioSaida() { return idHorarioSaida; }
    public void setIdHorarioSaida(Integer idHorarioSaida) { this.idHorarioSaida = idHorarioSaida; }

    // ====== Getters e Setters: Emissao / Assento ======

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public String getAssento() { return assento; }
    public void setAssento(String assento) { this.assento = assento; }

    public String getRequisicao() { return requisicao; }
    public void setRequisicao(String requisicao) { this.requisicao = requisicao; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

    // ====== Getters e Setters: Valores financeiros ======

    public BigDecimal getValorAlimentacao() { return valorAlimentacao; }
    public void setValorAlimentacao(BigDecimal valorAlimentacao) { this.valorAlimentacao = valorAlimentacao; }

    public BigDecimal getValorTransporte() { return valorTransporte; }
    public void setValorTransporte(BigDecimal valorTransporte) { this.valorTransporte = valorTransporte; }

    public BigDecimal getValorCargas() { return valorCargas; }
    public void setValorCargas(BigDecimal valorCargas) { this.valorCargas = valorCargas; }

    public BigDecimal getValorDescontoTarifa() { return valorDescontoTarifa; }
    public void setValorDescontoTarifa(BigDecimal valorDescontoTarifa) { this.valorDescontoTarifa = valorDescontoTarifa; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public BigDecimal getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) { this.valorDesconto = valorDesconto; }

    public BigDecimal getValorAPagar() { return valorAPagar; }
    public void setValorAPagar(BigDecimal valorAPagar) { this.valorAPagar = valorAPagar; }

    // ====== Getters e Setters: Pagamento misto ======

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    public BigDecimal getValorPagamentoDinheiro() { return valorPagamentoDinheiro != null ? valorPagamentoDinheiro : BigDecimal.ZERO; }
    public void setValorPagamentoDinheiro(BigDecimal valorPagamentoDinheiro) { this.valorPagamentoDinheiro = valorPagamentoDinheiro; }

    public BigDecimal getValorPagamentoCartao() { return valorPagamentoCartao != null ? valorPagamentoCartao : BigDecimal.ZERO; }
    public void setValorPagamentoCartao(BigDecimal valorPagamentoCartao) { this.valorPagamentoCartao = valorPagamentoCartao; }

    public BigDecimal getValorPagamentoPix() { return valorPagamentoPix != null ? valorPagamentoPix : BigDecimal.ZERO; }
    public void setValorPagamentoPix(BigDecimal valorPagamentoPix) { this.valorPagamentoPix = valorPagamentoPix; }

    public BigDecimal getTroco() { return troco; }
    public void setTroco(BigDecimal troco) { this.troco = troco; }

    public BigDecimal getDevedor() { return devedor; }
    public void setDevedor(BigDecimal devedor) { this.devedor = devedor; }

    // ====== Getters e Setters: Status / Controle ======

    public String getStatusPassagem() { return statusPassagem; }
    public void setStatusPassagem(String statusPassagem) { this.statusPassagem = statusPassagem; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    // ====== Getters e Setters: Display — Passageiro (JOIN) ======

    public String getNomePassageiro() { return nomePassageiro; }
    public void setNomePassageiro(String nomePassageiro) { this.nomePassageiro = nomePassageiro; }

    public String getNumeroDoc() { return numeroDoc; }
    public void setNumeroDoc(String numeroDoc) { this.numeroDoc = numeroDoc; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    public int getIdade() { return idade; }
    public void setIdade(int idade) { this.idade = idade; }

    // ====== Getters e Setters: Display — Viagem / Rota (JOIN) ======

    public LocalDate getDataViagem() { return dataViagem; }
    public void setDataViagem(LocalDate dataViagem) { this.dataViagem = dataViagem; }

    public String getDescricaoHorarioSaida() { return descricaoHorarioSaida; }
    public void setDescricaoHorarioSaida(String descricaoHorarioSaida) { this.descricaoHorarioSaida = descricaoHorarioSaida; }

    public String getHorarioSaidaStr() {
        return descricaoHorarioSaida != null ? descricaoHorarioSaida : "N/A";
    }
    public void setHorarioSaidaStr(String horarioSaidaStr) {
        this.descricaoHorarioSaida = horarioSaidaStr;
    }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public String getStrViagem() { return strViagem; }
    public void setStrViagem(String strViagem) { this.strViagem = strViagem; }

    // ====== Getters e Setters: Display — Auxiliares (JOIN) ======

    public String getAcomodacao() { return acomodacao; }
    public void setAcomodacao(String acomodacao) { this.acomodacao = acomodacao; }

    public String getTipoPassagemAux() { return tipoPassagemAux; }
    public void setTipoPassagemAux(String tipoPassagemAux) { this.tipoPassagemAux = tipoPassagemAux; }

    public String getAgenteAux() { return agenteAux; }
    public void setAgenteAux(String agenteAux) { this.agenteAux = agenteAux; }

    /**
     * Getter inteligente para forma de pagamento.
     * Se ja tiver um valor texto definido (ex: carregado do banco antigo), usa ele.
     * Senao, calcula a partir dos campos {@code valorPagamentoDinheiro/Cartao/Pix}.
     */
    public String getFormaPagamento() {
        // Se ja tiver uma forma de pagamento "texto" definida (ex: carregada do banco antigo), usa ela
        if (this.formaPagamento != null && !this.formaPagamento.isEmpty()) {
            return this.formaPagamento;
        }

        // Senao, calcula baseado nos valores
        boolean temDinheiro = getValorPagamentoDinheiro().compareTo(BigDecimal.ZERO) > 0;
        boolean temPix = getValorPagamentoPix().compareTo(BigDecimal.ZERO) > 0;
        boolean temCartao = getValorPagamentoCartao().compareTo(BigDecimal.ZERO) > 0;

        int qtd = 0;
        if (temDinheiro) qtd++;
        if (temPix) qtd++;
        if (temCartao) qtd++;

        if (qtd > 1) return "MISTO";
        if (temDinheiro) return "DINHEIRO";
        if (temPix) return "PIX";
        if (temCartao) return "CARTÃO";

        return "PENDENTE";
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getCaixa() { return caixa; }
    public void setCaixa(String caixa) { this.caixa = caixa; }

    // ====== Object overrides ======

    @Override
    public String toString() {
        return "Passagem{id=" + id + ", numBilhete=" + numBilhete + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passagem passagem = (Passagem) o;
        return Objects.equals(id, passagem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
