package com.naviera.api.controller;

import com.naviera.api.service.TarifaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/tarifas")
public class TarifaController {
    private final TarifaService service;
    public TarifaController(TarifaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(service.listarPorRota()); }
}
