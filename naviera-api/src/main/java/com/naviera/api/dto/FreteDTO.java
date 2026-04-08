package com.naviera.api.dto;
import java.math.BigDecimal;
public record FreteDTO(Long id, String numeroFrete, String remetente, String destinatario, String rota,
    String embarcacao, BigDecimal valorNominal, BigDecimal valorPago, BigDecimal valorDevedor,
    String status, int totalVolumes, String peso, String dataViagem) {}
