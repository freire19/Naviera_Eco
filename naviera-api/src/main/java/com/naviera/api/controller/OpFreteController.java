package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.OpFreteService;
import com.naviera.api.service.OpFreteWriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/op/fretes")
public class OpFreteController {
    private final OpFreteService service;
    private final OpFreteWriteService writeService;

    public OpFreteController(OpFreteService service, OpFreteWriteService writeService) {
        this.service = service;
        this.writeService = writeService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(name = "viagem_id", required = false) Long viagemId, Authentication auth) {
        return ResponseEntity.ok(service.listar(TenantUtils.getEmpresaId(auth), viagemId));
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestParam(name = "viagem_id") Long viagemId, Authentication auth) {
        if (viagemId == null) throw ApiException.badRequest("viagem_id obrigatorio");
        return ResponseEntity.ok(service.resumo(TenantUtils.getEmpresaId(auth), viagemId));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criar(TenantUtils.getEmpresaId(auth), dados));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizar(TenantUtils.getEmpresaId(auth), id, dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(writeService.excluir(TenantUtils.getEmpresaId(auth), id));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.pagar(TenantUtils.getEmpresaId(auth), id, dados));
    }
}
