package com.naviera.api.dto;
import java.math.BigDecimal;
public record LojaDTO(Long id, String nomeFantasia, String segmento, String[] rotasAtendidas,
    boolean verificada, Integer totalEntregas, BigDecimal notaMedia, String telefone, String email) {}
