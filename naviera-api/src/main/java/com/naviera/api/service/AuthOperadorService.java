package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.AuthOperadorResponse;
import com.naviera.api.dto.LoginOperadorRequest;
import com.naviera.api.dto.UsuarioDTO;
import com.naviera.api.repository.UsuarioRepository;
import com.naviera.api.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthOperadorService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthOperadorService(UsuarioRepository repo, PasswordEncoder encoder, JwtUtil jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthOperadorResponse login(LoginOperadorRequest req) {
        if (req.login() == null || req.login().isBlank() || req.senha() == null || req.senha().isBlank()) {
            throw ApiException.badRequest("Login e senha obrigatorios");
        }

        var usuario = repo.findByLogin(req.login())
            .orElseThrow(() -> ApiException.unauthorized("Credenciais invalidas"));

        if (!encoder.matches(req.senha(), usuario.getSenha())) {
            throw ApiException.unauthorized("Credenciais invalidas");
        }

        String token = jwt.gerarTokenOperador(usuario.getId(), usuario.getNome(), usuario.getFuncao(), usuario.getEmpresaId());

        var dto = new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getFuncao(),
            usuario.getPermissao()
        );

        boolean deveTrocar = Boolean.TRUE.equals(usuario.getDeveTrocarSenha());

        return new AuthOperadorResponse(token, deveTrocar, dto);
    }

    public UsuarioDTO me(Integer usuarioId) {
        var usuario = repo.findById(usuarioId)
            .orElseThrow(() -> ApiException.notFound("Usuario nao encontrado"));

        return new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getFuncao(),
            usuario.getPermissao()
        );
    }
}
