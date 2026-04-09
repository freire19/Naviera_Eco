package com.naviera.api.dto;
public record EmbarcacaoDTO(Long id, String nome, Integer capacidadePassageiros, String status, String rotaAtual,
    String dataViagem, String previsaoChegada, String fotoUrl, String linkExterno, String descricao,
    String rotaPrincipal, String horarioSaidaPadrao, String telefone) {}
