package com.naviera.api.dto;
import java.math.BigDecimal;
public record PedidoDTO(Long id, String numeroPedido, String clienteNome, String cidadeDestino,
    String descricaoItens, BigDecimal valorTotal, String status, Long idFrete,
    String codigoRastreio, String dataPedido) {}
