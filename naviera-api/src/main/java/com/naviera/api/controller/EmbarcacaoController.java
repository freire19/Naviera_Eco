package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.EmbarcacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/embarcacoes")
public class EmbarcacaoController {
    private final EmbarcacaoService service;
    public EmbarcacaoController(EmbarcacaoService service) { this.service = service; }

    // #DB143: pass empresaId from JWT to service
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.listarComStatus(empresaId));
    }
}
