package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Passagem {
    // --- Atributos de persistência (mapeiam diretamente para colunas do DB) ---
    private Long id; // id_passagem (PK)
    private Integer numBilhete; // numero_bilhete

    // FKs (IDs) - Usamos Long para consistência com IDs de DB, Integer para int do DB
    private Long idPassageiro; // fk para passageiros
    private Long idViagem; // fk para viagens
    private Long idRota; // fk para rotas (da viagem ou da passagem)
    private Integer idAcomodacao; // fk para aux_acomodacoes (Assumindo INTEGER no DB)
    private Integer idTipoPassagem; // fk para aux_tipos_passagem (Assumindo INTEGER no DB)
    private Integer idAgente; // fk para aux_agentes (Assumindo INTEGER no DB)
    private Integer idFormaPagamento; // fk para aux_formas_pagamento (Assumindo INTEGER no DB)
    private Integer idCaixa; // fk para caixas (Assumindo INTEGER no DB)
    private Integer idUsuarioEmissor; // fk para usuarios (Assumindo INTEGER no DB)
    private Integer idHorarioSaida; // fk para aux_horarios_saida (Assumindo INTEGER no DB)

    // --- Outros atributos persistentes da Passagem ---
    private LocalDate dataEmissao; // data_emissao
    private String assento;
    private String requisicao; // numero_requisicao
    private BigDecimal valorAlimentacao; // valor_alimentacao
    private BigDecimal valorTransporte; // valor_transporte
    private BigDecimal valorCargas; // valor_cargas
    private BigDecimal valorDescontoTarifa; // valor_desconto_tarifa
    private BigDecimal valorTotal; // valor_total
    private BigDecimal valorDesconto; // valor_desconto_geral
    private BigDecimal valorAPagar; // valor_a_pagar
    private BigDecimal valorPago; // valor_pago
    private BigDecimal troco;
    private BigDecimal devedor; // valor_devedor
    private String statusPassagem; // status_passagem
    private String observacoes;

    // --- Atributos para exibição em GUI (obtidos via JOINs ou cálculo) ---
    private String nomePassageiro; // nome_passageiro do Passageiro
    private String numeroDoc; // numero_documento do Passageiro
    private LocalDate dataNascimento; // data_nascimento do Passageiro
    private String sexo; // nome_sexo do Passageiro
    private String tipoDoc; // nome_tipo_doc do Passageiro
    private String nacionalidade; // nome_nacionalidade do Passageiro
    private int idade; // Idade calculada

    private LocalDate dataViagem; // data_viagem da Viagem
    private String descricaoHorarioSaida; // descricao_horario_saida da aux_horarios_saida
    private String origem; // origem da Rota
    private String destino; // destino da Rota
    private String strViagem; // Representação em String da viagem (para ComboBox)

    // NOVOS ATRIBUTOS PARA ARMAZENAR OS NOMES DAS AUXILIARES PARA USO NA GUI E DAO.
    private String acomodacao; // Nome da acomodação (ex: "Poltrona")
    private String tipoPassagemAux; // Nome do tipo de passagem (ex: "Ida", "Ida e Volta")
    private String agenteAux; // Nome do agente (ex: "Venda Direta", "Agência X")
    private String formaPagamento; // Nome da forma de pagamento (ex: "Dinheiro", "Cartão")
    private String caixa; // Nome do caixa (ex: "Caixa Principal")

    // --- Atributo para número sequencial em listas (coluna ORD) ---
    private Integer ordem;

    // --- Construtor vazio (essencial) ---
    public Passagem() {}

    // --- Getters e Setters (todos) ---
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

    public Integer getIdFormaPagamento() { return idFormaPagamento; }
    public void setIdFormaPagamento(Integer idFormaPagamento) { this.idFormaPagamento = idFormaPagamento; }

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

    public BigDecimal getTroco() { return troco; }
    public void setTroco(BigDecimal troco) { this.troco = troco; }

    public BigDecimal getDevedor() { return devedor; }
    public void setDevedor(BigDecimal devedor) { this.devedor = devedor; }

    public String getStatusPassagem() { return statusPassagem; }
    public void setStatusPassagem(String statusPassagem) { this.statusPassagem = statusPassagem; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    // Atributos para exibição em GUI
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

    // Conveniência para FXML/Strings formatadas (mantido para compatibilidade, mas o DAO usará descricaoHorarioSaida)
    public String getHorarioSaidaStr() {
        return descricaoHorarioSaida != null ? descricaoHorarioSaida : "N/A";
    }
    // Adicionado setter para HorarioSaidaStr se for usado em alguma tela de edição, embora seja redundante com setDescricaoHorarioSaida
    public void setHorarioSaidaStr(String horarioSaidaStr) {
        this.descricaoHorarioSaida = horarioSaidaStr;
    }


    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public String getStrViagem() { return strViagem; }
    public void setStrViagem(String strViagem) { this.strViagem = strViagem; }

    // NOVOS GETTERS E SETTERS PARA OS NOMES DAS TABELAS AUXILIARES
    public String getAcomodacao() {
        return acomodacao;
    }

    public void setAcomodacao(String acomodacao) {
        this.acomodacao = acomodacao;
    }

    public String getTipoPassagemAux() { // Renomeado de getPassagemAux para maior clareza
        return tipoPassagemAux;
    }

    public void setTipoPassagemAux(String tipoPassagemAux) { // Renomeado de setPassagemAux
        this.tipoPassagemAux = tipoPassagemAux;
    }

    public String getAgenteAux() {
        return agenteAux;
    }

    public void setAgenteAux(String agenteAux) {
        this.agenteAux = agenteAux;
    }

    public String getFormaPagamento() { // Renomeado de getTipoPagamento para consistência
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) { // Renomeado de setTipoPagamento
        this.formaPagamento = formaPagamento;
    }

    public String getCaixa() {
        return caixa;
    }

    public void setCaixa(String caixa) {
        this.caixa = caixa;
    }

    // Atributo para número sequencial em listas (coluna ORD)
    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

    // Método toString para depuração ou exibição
    @Override
    public String toString() {
        return "Passagem{" +
                "id=" + id +
                ", numBilhete=" + numBilhete +
                ", nomePassageiro='" + nomePassageiro + '\'' +
                ", dataViagem=" + dataViagem +
                ", horarioSaida='" + descricaoHorarioSaida + '\'' +
                ", origem='" + origem + '\'' +
                ", destino='" + destino + '\'' +
                ", valorTotal=" + valorTotal +
                '}';
    }
}