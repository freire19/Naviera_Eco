package com.naviera.api.controller;

import com.naviera.api.service.ViagemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/viagens")
public class ViagemController {
    private final ViagemService service;
    public ViagemController(ViagemService service) { this.service = service; }

    @GetMapping("/ativas")
    public ResponseEntity<?> ativas() { return ResponseEntity.ok(service.buscarAtivas()); }

    @GetMapping("/embarcacao/{id}")
    public ResponseEntity<?> porEmbarcacao(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorEmbarcacao(id));
    }

    /** Viagens ativas de todas as empresas (público, sem autenticação) */
    @GetMapping("/publicas")
    public ResponseEntity<?> publicas() { return ResponseEntity.ok(service.buscarPublicas()); }
}
