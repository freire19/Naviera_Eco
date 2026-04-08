package com.naviera.api.service;

import com.naviera.api.dto.*;
import com.naviera.api.model.ClienteApp;
import com.naviera.api.repository.ClienteAppRepository;
import com.naviera.api.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.naviera.api.config.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private final ClienteAppRepository repo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthService(ClienteAppRepository repo, PasswordEncoder encoder, JwtUtil jwt) {
        this.repo = repo; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        var cliente = repo.findByDocumentoAndAtivoTrue(req.documento())
            .orElseThrow(() -> ApiException.unauthorized("Documento nao encontrado"));
        if (!encoder.matches(req.senha(), cliente.getSenhaHash()))
            throw ApiException.unauthorized("Senha incorreta");
        cliente.setUltimoAcesso(LocalDateTime.now());
        repo.save(cliente);
        String token = jwt.gerarToken(cliente.getId(), cliente.getDocumento(), cliente.getTipoDocumento());
        return new AuthResponse(token, cliente.getTipoDocumento(), cliente.getNome(), cliente.getId());
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest req) {
        if (repo.existsByDocumento(req.documento()))
            throw ApiException.conflict("Documento ja cadastrado");
        var c = new ClienteApp();
        c.setDocumento(req.documento());
        c.setTipoDocumento(req.tipoDocumento() != null ? req.tipoDocumento() : "CPF");
        c.setNome(req.nome());
        c.setEmail(req.email());
        c.setTelefone(req.telefone());
        c.setCidade(req.cidade());
        c.setSenhaHash(encoder.encode(req.senha()));
        c.setAtivo(true);
        c = repo.save(c);
        String token = jwt.gerarToken(c.getId(), c.getDocumento(), c.getTipoDocumento());
        return new AuthResponse(token, c.getTipoDocumento(), c.getNome(), c.getId());
    }
}
