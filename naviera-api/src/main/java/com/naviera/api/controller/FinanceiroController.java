package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.FinanceiroService;
import com.naviera.api.service.FinanceiroWriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/op/financeiro")
public class FinanceiroController {
    private final FinanceiroService service;
    private final FinanceiroWriteService writeService;

    public FinanceiroController(FinanceiroService service, FinanceiroWriteService writeService) {
        this.service = service;
        this.writeService = writeService;
    }

    @GetMapping("/entradas")
    public ResponseEntity<?> entradas(@RequestParam(name = "viagem_id", required = false) Long viagemId, Authentication auth) {
        return ResponseEntity.ok(service.listarEntradas(TenantUtils.getEmpresaId(auth), viagemId));
    }

    @GetMapping("/saidas")
    public ResponseEntity<?> saidas(@RequestParam(name = "viagem_id", required = false) Long viagemId, Authentication auth) {
        return ResponseEntity.ok(service.listarSaidas(TenantUtils.getEmpresaId(auth), viagemId));
    }

    @GetMapping("/balanco")
    public ResponseEntity<?> balanco(@RequestParam(name = "viagem_id") Long viagemId, Authentication auth) {
        if (viagemId == null) throw ApiException.badRequest("viagem_id obrigatorio");
        return ResponseEntity.ok(service.balanco(TenantUtils.getEmpresaId(auth), viagemId));
    }

    @PostMapping("/saida")
    public ResponseEntity<?> criarSaida(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarSaida(TenantUtils.getEmpresaId(auth), dados));
    }

    @DeleteMapping("/saida/{id}")
    public ResponseEntity<?> excluirSaida(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(writeService.excluirSaida(TenantUtils.getEmpresaId(auth), id));
    }
}
