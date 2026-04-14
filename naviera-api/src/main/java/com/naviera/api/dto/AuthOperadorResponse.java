package com.naviera.api.dto;

public record AuthOperadorResponse(String token, boolean deveTrocarSenha, UsuarioDTO usuario) {}
