package model;

import java.math.BigDecimal;

public class Encomenda {

    private Long id;
    private Long idViagem;
    private String numeroEncomenda;
    private String remetente;
    private String destinatario;
    private String observacoes;
    private int totalVolumes;
    private BigDecimal totalAPagar;
    private BigDecimal valorPago;
    private BigDecimal desconto;
    
    // Campos de pagamento
    private String statusPagamento; 
    private String formaPagamento; 
    private String localPagamento; // CAMPO NOVO (Caixa)
    
    private Integer idCaixa;
    // #034: tipo seguro para data; getter String mantido para compatibilidade com PropertyValueFactory
    private java.time.LocalDate dataLancamentoDate;
    private String dataLancamento;
    
    // Campos de Entrega
    private boolean entregue;
    private String docRecebedor;  
    private String nomeRecebedor; 
    private String nomeRota;

    public Encomenda() {
        this.totalAPagar = BigDecimal.ZERO;
        this.valorPago = BigDecimal.ZERO;
        this.desconto = BigDecimal.ZERO;
        this.entregue = false;
    }

    public BigDecimal getTotalAPagar() { return totalAPagar == null ? BigDecimal.ZERO : totalAPagar; }
    public BigDecimal getValorPago() { return valorPago == null ? BigDecimal.ZERO : valorPago; }
    public BigDecimal getDesconto() { return desconto == null ? BigDecimal.ZERO : desconto; }
    
    public BigDecimal getSaldoDevedor() {
        BigDecimal total = getTotalAPagar().subtract(getDesconto());
        return total.subtract(getValorPago()).max(BigDecimal.ZERO);
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getIdViagem() { return idViagem; }
    public void setIdViagem(Long idViagem) { this.idViagem = idViagem; }
    
    public String getNumeroEncomenda() { return numeroEncomenda; }
    public void setNumeroEncomenda(String numeroEncomenda) { this.numeroEncomenda = numeroEncomenda; }
    
    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }
    
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    
    public int getTotalVolumes() { return totalVolumes; }
    public void setTotalVolumes(int totalVolumes) { this.totalVolumes = totalVolumes; }
    
    public void setTotalAPagar(BigDecimal totalAPagar) { this.totalAPagar = totalAPagar; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    
    public String getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(String statusPagamento) { this.statusPagamento = statusPagamento; }
    
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getLocalPagamento() { return localPagamento; }
    public void setLocalPagamento(String localPagamento) { this.localPagamento = localPagamento; }
    
    public Integer getIdCaixa() { return idCaixa; }
    public void setIdCaixa(Integer idCaixa) { this.idCaixa = idCaixa; }
    
    public String getDataLancamento() { return dataLancamento; }
    public void setDataLancamento(String dataLancamento) { this.dataLancamento = dataLancamento; }

    /** #034: getter/setter tipado para uso em novo codigo. */
    public java.time.LocalDate getDataLancamentoDate() { return dataLancamentoDate; }
    public void setDataLancamentoDate(java.time.LocalDate dataLancamentoDate) {
        this.dataLancamentoDate = dataLancamentoDate;
        this.dataLancamento = (dataLancamentoDate != null)
            ? dataLancamentoDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : null;
    }
    
    public boolean isEntregue() { return entregue; }
    public void setEntregue(boolean entregue) { this.entregue = entregue; }
    
    public String getDocRecebedor() { return docRecebedor; }
    public void setDocRecebedor(String docRecebedor) { this.docRecebedor = docRecebedor; }
    
    public String getNomeRecebedor() { return nomeRecebedor; }
    public void setNomeRecebedor(String nomeRecebedor) { this.nomeRecebedor = nomeRecebedor; }
    
    public String getNomeRota() { return nomeRota; }
    public void setNomeRota(String nomeRota) { this.nomeRota = nomeRota; }
}