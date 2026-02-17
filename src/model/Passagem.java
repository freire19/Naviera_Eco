package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects; 

public class Passagem {
    // --- Atributos de persistência ---
    private Long id; 
    private Integer numBilhete; 

    private Long idPassageiro; 
    private Long idViagem; 
    private Long idRota; 
    private Integer idAcomodacao; 
    private Integer idTipoPassagem; 
    private Integer idAgente; 
    
    // Mantido apenas para compatibilidade com código legado, mas não usamos mais para lógica nova
    private Integer idFormaPagamento; 
    
    private Integer idCaixa; 
    private Integer idUsuarioEmissor; 
    private Integer idHorarioSaida; 

    private LocalDate dataEmissao; 
    private String assento;
    private String requisicao; 
    private BigDecimal valorAlimentacao; 
    private BigDecimal valorTransporte; 
    private BigDecimal valorCargas; 
    private BigDecimal valorDescontoTarifa; 
    private BigDecimal valorTotal; 
    private BigDecimal valorDesconto; 
    private BigDecimal valorAPagar; 
    
    // --- NOVOS CAMPOS MISTOS ---
    private BigDecimal valorPago; 
    private BigDecimal valorPagamentoDinheiro = BigDecimal.ZERO; 
    private BigDecimal valorPagamentoCartao = BigDecimal.ZERO;   
    private BigDecimal valorPagamentoPix = BigDecimal.ZERO;      
    
    private BigDecimal troco;
    private BigDecimal devedor; 
    private String statusPassagem; 
    private String observacoes;

    // --- Atributos de exibição ---
    private String nomePassageiro; 
    private String numeroDoc; 
    private LocalDate dataNascimento; 
    private String sexo; 
    private String tipoDoc; 
    private String nacionalidade; 
    private int idade; 

    private LocalDate dataViagem; 
    private String descricaoHorarioSaida; 
    private String origem; 
    private String destino; 
    private String strViagem; 

    private String acomodacao; 
    private String tipoPassagemAux; 
    private String agenteAux; 
    
    // Vamos manter a variável interna, mas o GETTER será inteligente
    private String formaPagamento; 
    
    private String caixa; 
    private Integer ordem;

    public Passagem() {}

    // --- Getters e Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNumBilhete() { return numBilhete; }
    public void setNumBilhete(Integer numBilhete) { this.numBilhete = numBilhete; }

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

    // >>> MÉTODOS REINTEGRADOS PARA NÃO QUEBRAR O DAO <<<
    public Integer getIdFormaPagamento() { return idFormaPagamento; }
    public void setIdFormaPagamento(Integer idFormaPagamento) { this.idFormaPagamento = idFormaPagamento; }
    // ----------------------------------------------------

    public Integer getIdCaixa() { return idCaixa; }
    public void setIdCaixa(Integer idCaixa) { this.idCaixa = idCaixa; }

    public Integer getIdUsuarioEmissor() { return idUsuarioEmissor; }
    public void setIdUsuarioEmissor(Integer idUsuarioEmissor) { this.idUsuarioEmissor = idUsuarioEmissor; }

    public Integer getIdHorarioSaida() { return idHorarioSaida; }
    public void setIdHorarioSaida(Integer idHorarioSaida) { this.idHorarioSaida = idHorarioSaida; }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public String getAssento() { return assento; }
    public void setAssento(String assento) { this.assento = assento; }

    public String getRequisicao() { return requisicao; }
    public void setRequisicao(String requisicao) { this.requisicao = requisicao; }

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

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    // --- NOVOS GETTERS E SETTERS PAGAMENTO MISTO ---
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

    public String getStatusPassagem() { return statusPassagem; }
    public void setStatusPassagem(String statusPassagem) { this.statusPassagem = statusPassagem; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

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

    public String getAcomodacao() { return acomodacao; }
    public void setAcomodacao(String acomodacao) { this.acomodacao = acomodacao; }

    public String getTipoPassagemAux() { return tipoPassagemAux; }
    public void setTipoPassagemAux(String tipoPassagemAux) { this.tipoPassagemAux = tipoPassagemAux; }

    public String getAgenteAux() { return agenteAux; }
    public void setAgenteAux(String agenteAux) { this.agenteAux = agenteAux; }

    // >>> CORREÇÃO DO RELATÓRIO: GET INTELIGENTE <<<
    public String getFormaPagamento() {
        // Se já tiver uma forma de pagamento "texto" definida (ex: carregada do banco antigo), usa ela
        if (this.formaPagamento != null && !this.formaPagamento.isEmpty()) {
            return this.formaPagamento;
        }
        
        // Senão, calcula baseado nos valores
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

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

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