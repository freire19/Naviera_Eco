package com.naviera.api.controller;

import com.naviera.api.service.EmbarcacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/embarcacoes")
public class EmbarcacaoController {
    private final EmbarcacaoService service;
    public EmbarcacaoController(EmbarcacaoService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(service.listarComStatus()); }
}
