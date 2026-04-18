package com.naviera.api.psp;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Log de cobrancas criadas no PSP. Espelha a tabela psp_cobrancas (migration 030).
 */
@Entity
@Table(name = "psp_cobrancas")
public class PspCobranca {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Integer empresaId;

    @Column(name = "tipo_origem", nullable = false, length = 20)
    private String tipoOrigem;

    @Column(name = "origem_id", nullable = false)
    private Long origemId;

    @Column(name = "psp_provider", nullable = false, length = 30)
    private String pspProvider;

    @Column(name = "psp_cobranca_id", nullable = false, length = 100)
    private String pspCobrancaId;

    @Column(name = "psp_status", nullable = false, length = 30)
    private String pspStatus = "PENDENTE";

    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private String formaPagamento;

    @Column(name = "valor_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "desconto_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal descontoAplicado = BigDecimal.ZERO;

    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "split_naviera_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal splitNavieraPct = new BigDecimal("1.50");

    @Column(name = "split_naviera_valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal splitNavieraValor = BigDecimal.ZERO;

    @Column(name = "split_empresa_valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal splitEmpresaValor = BigDecimal.ZERO;

    @Column(name = "qr_code_payload", columnDefinition = "text")
    private String qrCodePayload;

    @Column(name = "qr_code_image_url", columnDefinition = "text")
    private String qrCodeImageUrl;

    @Column(name = "linha_digitavel", length = 100)
    private String linhaDigitavel;

    @Column(name = "boleto_url", columnDefinition = "text")
    private String boletoUrl;

    @Column(name = "checkout_url", columnDefinition = "text")
    private String checkoutUrl;

    @Column(name = "cliente_app_id")
    private Long clienteAppId;

    @Column(name = "data_criacao", insertable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_confirmacao")
    private LocalDateTime dataConfirmacao;

    @Column(name = "data_vencimento")
    private LocalDateTime dataVencimento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    public Long getId() { return id; }
    public Integer getEmpresaId() { return empresaId; } public void setEmpresaId(Integer v) { this.empresaId = v; }
    public String getTipoOrigem() { return tipoOrigem; } public void setTipoOrigem(String v) { this.tipoOrigem = v; }
    public Long getOrigemId() { return origemId; } public void setOrigemId(Long v) { this.origemId = v; }
    public String getPspProvider() { return pspProvider; } public void setPspProvider(String v) { this.pspProvider = v; }
    public String getPspCobrancaId() { return pspCobrancaId; } public void setPspCobrancaId(String v) { this.pspCobrancaId = v; }
    public String getPspStatus() { return pspStatus; } public void setPspStatus(String v) { this.pspStatus = v; }
    public String getFormaPagamento() { return formaPagamento; } public void setFormaPagamento(String v) { this.formaPagamento = v; }
    public BigDecimal getValorBruto() { return valorBruto; } public void setValorBruto(BigDecimal v) { this.valorBruto = v; }
    public BigDecimal getDescontoAplicado() { return descontoAplicado; } public void setDescontoAplicado(BigDecimal v) { this.descontoAplicado = v; }
    public BigDecimal getValorLiquido() { return valorLiquido; } public void setValorLiquido(BigDecimal v) { this.valorLiquido = v; }
    public BigDecimal getSplitNavieraPct() { return splitNavieraPct; } public void setSplitNavieraPct(BigDecimal v) { this.splitNavieraPct = v; }
    public BigDecimal getSplitNavieraValor() { return splitNavieraValor; } public void setSplitNavieraValor(BigDecimal v) { this.splitNavieraValor = v; }
    public BigDecimal getSplitEmpresaValor() { return splitEmpresaValor; } public void setSplitEmpresaValor(BigDecimal v) { this.splitEmpresaValor = v; }
    public String getQrCodePayload() { return qrCodePayload; } public void setQrCodePayload(String v) { this.qrCodePayload = v; }
    public String getQrCodeImageUrl() { return qrCodeImageUrl; } public void setQrCodeImageUrl(String v) { this.qrCodeImageUrl = v; }
    public String getLinhaDigitavel() { return linhaDigitavel; } public void setLinhaDigitavel(String v) { this.linhaDigitavel = v; }
    public String getBoletoUrl() { return boletoUrl; } public void setBoletoUrl(String v) { this.boletoUrl = v; }
    public String getCheckoutUrl() { return checkoutUrl; } public void setCheckoutUrl(String v) { this.checkoutUrl = v; }
    public Long getClienteAppId() { return clienteAppId; } public void setClienteAppId(Long v) { this.clienteAppId = v; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getDataConfirmacao() { return dataConfirmacao; } public void setDataConfirmacao(LocalDateTime v) { this.dataConfirmacao = v; }
    public LocalDateTime getDataVencimento() { return dataVencimento; } public void setDataVencimento(LocalDateTime v) { this.dataVencimento = v; }
    public String getRawResponse() { return rawResponse; } public void setRawResponse(String v) { this.rawResponse = v; }
}
