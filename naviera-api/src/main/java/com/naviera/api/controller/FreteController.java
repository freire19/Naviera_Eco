package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.service.FreteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/fretes")
public class FreteController {
    private final FreteService service;
    public FreteController(FreteService service) { this.service = service; }

    // #DB144: empresaId required as query param to prevent cross-tenant LIKE scan
    @GetMapping
    public ResponseEntity<?> meusFretes(Authentication auth,
            @RequestParam(value = "empresa_id", required = false) Integer empresaId) {
        if (empresaId == null) throw ApiException.badRequest("empresa_id obrigatorio");
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.buscarPorRemetente(id, empresaId));
    }
}
