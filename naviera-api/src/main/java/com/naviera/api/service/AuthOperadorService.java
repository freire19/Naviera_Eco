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

        // #DB145: empresa_id obrigatorio — sem ele o login poderia cruzar tenants
        if (req.empresa_id() == null) throw ApiException.badRequest("empresa_id obrigatorio para login");

        var usuario = repo.findByLoginAndEmpresa(req.login(), req.empresa_id())
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

    // DS4-023 fix: filtrar por empresa_id para defense-in-depth
    public UsuarioDTO me(Integer usuarioId, Integer empresaId) {
        var usuario = repo.findById(usuarioId)
            .filter(u -> empresaId == null || empresaId.equals(u.getEmpresaId()))
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
