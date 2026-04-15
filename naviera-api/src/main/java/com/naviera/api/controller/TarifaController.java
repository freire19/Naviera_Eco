package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.TarifaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/tarifas")
public class TarifaController {
    private final TarifaService service;
    public TarifaController(TarifaService service) { this.service = service; }

    // #DB143: pass empresaId from JWT to service
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.listarPorRota(empresaId));
    }
}
