package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.repository.RotaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/rotas")
public class RotaController {
    private final RotaRepository repo;
    public RotaController(RotaRepository repo) { this.repo = repo; }

    // #105: findAll vazava rotas cross-tenant. Agora filtra por empresa_id do JWT do operador.
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaIdOrNull(auth);
        if (empresaId == null) throw ApiException.forbidden("Endpoint restrito a operadores");
        return ResponseEntity.ok(repo.findByEmpresaId(empresaId));
    }
}
