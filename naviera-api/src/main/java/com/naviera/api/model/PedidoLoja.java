package com.naviera.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "pedidos_loja")
public class PedidoLoja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "numero_pedido", unique = true) private String numeroPedido;
    @Column(name = "id_loja") private Long idLoja;
    @Column(name = "id_cliente_comprador") private Long idClienteComprador;
    @Column(name = "cidade_destino") private String cidadeDestino;
    @Column(name = "descricao_itens") private String descricaoItens;
    @Column(name = "valor_total") private BigDecimal valorTotal;
    private String status = "AGUARDANDO_EMBARQUE";
    @Column(name = "id_frete") private Long idFrete;
    @Column(name = "codigo_rastreio") private String codigoRastreio;
    private String observacoes;
    @Column(name = "data_pedido") private LocalDateTime dataPedido;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNumeroPedido() { return numeroPedido; } public void setNumeroPedido(String n) { this.numeroPedido = n; }
    public Long getIdLoja() { return idLoja; } public void setIdLoja(Long i) { this.idLoja = i; }
    public Long getIdClienteComprador() { return idClienteComprador; } public void setIdClienteComprador(Long i) { this.idClienteComprador = i; }
    public String getCidadeDestino() { return cidadeDestino; } public void setCidadeDestino(String c) { this.cidadeDestino = c; }
    public String getDescricaoItens() { return descricaoItens; } public void setDescricaoItens(String d) { this.descricaoItens = d; }
    public BigDecimal getValorTotal() { return valorTotal; } public void setValorTotal(BigDecimal v) { this.valorTotal = v; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public Long getIdFrete() { return idFrete; } public void setIdFrete(Long i) { this.idFrete = i; }
    public String getCodigoRastreio() { return codigoRastreio; } public void setCodigoRastreio(String c) { this.codigoRastreio = c; }
    public LocalDateTime getDataPedido() { return dataPedido; }
}
