package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/op/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestParam(name = "viagem_id") Long viagemId, Authentication auth) {
        if (viagemId == null) throw ApiException.badRequest("viagem_id obrigatorio");
        return ResponseEntity.ok(service.resumo(TenantUtils.getEmpresaId(auth), viagemId));
    }
}
