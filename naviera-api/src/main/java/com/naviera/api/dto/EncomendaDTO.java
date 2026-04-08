package com.naviera.api.dto;
import java.math.BigDecimal;
public record EncomendaDTO(Long id, String numeroEncomenda, String remetente, String destinatario, String rota,
    String embarcacao, BigDecimal totalAPagar, BigDecimal valorPago, BigDecimal saldoDevedor,
    String statusPagamento, boolean entregue, int totalVolumes, String dataViagem, String previsaoChegada) {}
