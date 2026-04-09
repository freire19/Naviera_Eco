package com.naviera.api.controller;

import com.naviera.api.dto.CompraPassagemRequest;
import com.naviera.api.service.PassagemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/passagens")
public class PassagemController {
    private final PassagemService service;
    public PassagemController(PassagemService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> minhas(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.minhasPassagens(id));
    }

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(Authentication auth, @RequestBody CompraPassagemRequest req) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.comprar(id, req));
    }
}
