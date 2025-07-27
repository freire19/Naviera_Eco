package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo mínimo para Encomenda, contendo todos os getters/setters usados no Controller.
 */
public class Encomenda {
    private int id;
    private String numeroEncomenda;
    private String remetente;
    private String destinatario;
    private String docRecebedor;
    private String observacao;
    private String viagem;
    private BigDecimal valorNominal;
    private BigDecimal desconto;
    private BigDecimal valorPago;
    private BigDecimal valorAPagar;
    private BigDecimal devedor;
    private String tipoPagamento;
    private String caixa;
    private LocalDate data;
    private boolean entregue;
    private LocalDate dataEntrega;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getNumeroEncomenda() {
        return numeroEncomenda;
    }
    public void setNumeroEncomenda(String numeroEncomenda) {
        this.numeroEncomenda = numeroEncomenda;
    }

    public String getRemetente() {
        return remetente;
    }
    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public String getDestinatario() {
        return destinatario;
    }
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getDocRecebedor() {
        return docRecebedor;
    }
    public void setDocRecebedor(String docRecebedor) {
        this.docRecebedor = docRecebedor;
    }

    public String getObservacao() {
        return observacao;
    }
    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getViagem() {
        return viagem;
    }
    public void setViagem(String viagem) {
        this.viagem = viagem;
    }

    public BigDecimal getValorNominal() {
        return valorNominal;
    }
    public void setValorNominal(BigDecimal valorNominal) {
        this.valorNominal = valorNominal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }
    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }
    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public BigDecimal getValorAPagar() {
        return valorAPagar;
    }
    public void setValorAPagar(BigDecimal valorAPagar) {
        this.valorAPagar = valorAPagar;
    }

    public BigDecimal getDevedor() {
        return devedor;
    }
    public void setDevedor(BigDecimal devedor) {
        this.devedor = devedor;
    }

    public String getTipoPagamento() {
        return tipoPagamento;
    }
    public void setTipoPagamento(String tipoPagamento) {
        this.tipoPagamento = tipoPagamento;
    }

    public String getCaixa() {
        return caixa;
    }
    public void setCaixa(String caixa) {
        this.caixa = caixa;
    }

    public LocalDate getData() {
        return data;
    }
    public void setData(LocalDate data) {
        this.data = data;
    }

    public boolean isEntregue() {
        return entregue;
    }
    public void setEntregue(boolean entregue) {
        this.entregue = entregue;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }
    public void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }
}
