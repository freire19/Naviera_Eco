package com.naviera.api.dto;
public record RegisterRequest(String documento, String tipoDocumento, String nome, String email, String telefone, String cidade, String senha) {}
