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

    // #126: hash dummy fixo do BCrypt para equalizar tempo de resposta quando documento nao existe.
    //   Sem isso, atacante mede latencia (com BCrypt vs sem) e enumera CPFs/CNPJs cadastrados.
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    // #DP070: sem @Transactional — login e 1 SELECT + 1 UPDATE de ultimo_acesso fire-and-forget;
    //   nao precisa de ACID e overhead de tx era ~0.5ms desnecessario por login.
    public AuthResponse login(LoginRequest req) {
        var clienteOpt = repo.findByDocumentoAndAtivoTrue(req.documento());
        String hashAlvo = clienteOpt.map(ClienteApp::getSenhaHash).orElse(DUMMY_HASH);
        boolean senhaOk = encoder.matches(req.senha(), hashAlvo);
        if (clienteOpt.isEmpty() || !senhaOk) {
            throw ApiException.unauthorized("Credenciais invalidas");
        }
        var cliente = clienteOpt.get();
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
