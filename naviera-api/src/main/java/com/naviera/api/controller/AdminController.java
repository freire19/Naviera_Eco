package com.naviera.api.controller;

import com.naviera.api.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/empresas")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(service.listarEmpresas());
    }

    @GetMapping("/empresas/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarEmpresa(id));
    }

    @PostMapping("/empresas")
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> dados) {
        return ResponseEntity.ok(service.criarEmpresa(dados));
    }

    @PutMapping("/empresas/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
        return ResponseEntity.ok(service.atualizarEmpresa(id, dados));
    }

    @PutMapping("/empresas/{id}/ativar")
    public ResponseEntity<?> ativar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
        boolean ativo = Boolean.TRUE.equals(dados.get("ativo"));
        return ResponseEntity.ok(service.ativarEmpresa(id, ativo));
    }
}
