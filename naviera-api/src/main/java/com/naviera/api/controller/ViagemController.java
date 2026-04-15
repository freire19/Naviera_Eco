package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.ViagemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/viagens")
public class ViagemController {
    private final ViagemService service;
    public ViagemController(ViagemService service) { this.service = service; }

    // DS4-008 fix: operador ve so viagens da sua empresa, app user ve cross-tenant
    @GetMapping("/ativas")
    public ResponseEntity<?> ativas(Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaIdOrNull(auth);
        return ResponseEntity.ok(service.buscarAtivas(empresaId));
    }

    // DS4-008 fix: operador filtrado por empresa_id
    @GetMapping("/embarcacao/{id}")
    public ResponseEntity<?> porEmbarcacao(@PathVariable Long id, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaIdOrNull(auth);
        return ResponseEntity.ok(service.buscarPorEmbarcacao(id, empresaId));
    }

    /** Viagens ativas de todas as empresas (publico, sem autenticacao) */
    @GetMapping("/publicas")
    public ResponseEntity<?> publicas() { return ResponseEntity.ok(service.buscarPublicas()); }
}
