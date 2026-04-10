package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.OpViagemService;
import com.naviera.api.service.OpViagemWriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/op/viagens")
public class OpViagemController {
    private final OpViagemService service;
    private final OpViagemWriteService writeService;

    public OpViagemController(OpViagemService service, OpViagemWriteService writeService) {
        this.service = service;
        this.writeService = writeService;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        return ResponseEntity.ok(service.listarTodas(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/ativa")
    public ResponseEntity<?> ativa(Authentication auth) {
        return ResponseEntity.ok(service.buscarAtiva(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> porId(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.buscarPorId(TenantUtils.getEmpresaId(auth), id));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criar(TenantUtils.getEmpresaId(auth), dados));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizar(TenantUtils.getEmpresaId(auth), id, dados));
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<?> ativar(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        boolean ativa = Boolean.TRUE.equals(dados.get("ativa"));
        return ResponseEntity.ok(writeService.ativar(TenantUtils.getEmpresaId(auth), id, ativa));
    }
}
