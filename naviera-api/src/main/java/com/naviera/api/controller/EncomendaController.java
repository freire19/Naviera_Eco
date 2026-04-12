package com.naviera.api.controller;

import com.naviera.api.service.EncomendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/encomendas")
public class EncomendaController {
    private final EncomendaService service;
    public EncomendaController(EncomendaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> minhasEncomendas(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.buscarPorCliente(id));
    }

    /** Rastreio cross-tenant — encomendas do cliente logado em todas as empresas */
    @GetMapping("/rastreio")
    public ResponseEntity<?> rastreio(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.rastreioCrossTenant(id));
    }
}
