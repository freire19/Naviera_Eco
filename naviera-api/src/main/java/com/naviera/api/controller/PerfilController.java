package com.naviera.api.controller;

import com.naviera.api.dto.PerfilUpdateRequest;
import com.naviera.api.repository.ClienteAppRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/perfil")
public class PerfilController {
    private final ClienteAppRepository repo;
    public PerfilController(ClienteAppRepository repo) { this.repo = repo; }

    @GetMapping
    public ResponseEntity<?> meuPerfil(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return repo.findById(id)
            .map(c -> ResponseEntity.ok(Map.of(
                "id", c.getId(), "nome", c.getNome(), "documento", c.getDocumento(),
                "tipo", c.getTipoDocumento(), "email", c.getEmail() != null ? c.getEmail() : "",
                "telefone", c.getTelefone() != null ? c.getTelefone() : "",
                "cidade", c.getCidade() != null ? c.getCidade() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping @Transactional
    public ResponseEntity<?> atualizar(Authentication auth, @RequestBody @Valid PerfilUpdateRequest dados) {
        Long id = (Long) auth.getPrincipal();
        return repo.findById(id).map(c -> {
            if (dados.nome() != null) c.setNome(dados.nome());
            if (dados.email() != null) c.setEmail(dados.email());
            if (dados.telefone() != null) c.setTelefone(dados.telefone());
            if (dados.cidade() != null) c.setCidade(dados.cidade());
            repo.save(c);
            return ResponseEntity.ok(Map.of("mensagem", "Perfil atualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
