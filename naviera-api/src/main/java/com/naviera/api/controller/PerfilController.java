package com.naviera.api.controller;

import com.naviera.api.model.ClienteApp;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @PutMapping
    public ResponseEntity<?> atualizar(Authentication auth, @RequestBody Map<String, String> dados) {
        Long id = (Long) auth.getPrincipal();
        return repo.findById(id).map(c -> {
            if (dados.containsKey("nome")) c.setNome(dados.get("nome"));
            if (dados.containsKey("email")) c.setEmail(dados.get("email"));
            if (dados.containsKey("telefone")) c.setTelefone(dados.get("telefone"));
            if (dados.containsKey("cidade")) c.setCidade(dados.get("cidade"));
            repo.save(c);
            return ResponseEntity.ok(Map.of("mensagem", "Perfil atualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
