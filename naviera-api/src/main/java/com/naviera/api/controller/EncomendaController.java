package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.service.EncomendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/encomendas")
public class EncomendaController {
    private final EncomendaService service;
    public EncomendaController(EncomendaService service) { this.service = service; }

    // #DB144: empresaId required as query param to prevent cross-tenant LIKE scan
    @GetMapping
    public ResponseEntity<?> minhasEncomendas(Authentication auth,
            @RequestParam(value = "empresa_id", required = false) Integer empresaId) {
        if (empresaId == null) throw ApiException.badRequest("empresa_id obrigatorio");
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.buscarPorCliente(id, empresaId));
    }

    /** Rastreio cross-tenant — encomendas do cliente logado em todas as empresas */
    @GetMapping("/rastreio")
    public ResponseEntity<?> rastreio(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.rastreioCrossTenant(id));
    }
}
