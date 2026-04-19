package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.CadastrosService;
import com.naviera.api.service.CadastrosWriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/op/cadastros")
public class CadastrosController {
    private final CadastrosService service;
    private final CadastrosWriteService writeService;

    public CadastrosController(CadastrosService service, CadastrosWriteService writeService) {
        this.service = service;
        this.writeService = writeService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> usuarios(Authentication auth) {
        return ResponseEntity.ok(service.listarUsuarios(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/conferentes")
    public ResponseEntity<?> conferentes(Authentication auth) {
        return ResponseEntity.ok(service.listarConferentes(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/caixas")
    public ResponseEntity<?> caixas(Authentication auth) {
        return ResponseEntity.ok(service.listarCaixas(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/tarifas")
    public ResponseEntity<?> tarifas(Authentication auth) {
        return ResponseEntity.ok(service.listarTarifas(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/tipos-passageiro")
    public ResponseEntity<?> tiposPassageiro(Authentication auth) {
        return ResponseEntity.ok(service.listarTiposPassageiro(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/empresa")
    public ResponseEntity<?> empresa(Authentication auth) {
        return ResponseEntity.ok(service.buscarEmpresa(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/clientes-encomenda")
    public ResponseEntity<?> clientesEncomenda(Authentication auth) {
        return ResponseEntity.ok(service.listarClientesEncomenda(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/itens-encomenda")
    public ResponseEntity<?> itensEncomenda(Authentication auth) {
        return ResponseEntity.ok(service.listarItensEncomenda(TenantUtils.getEmpresaId(auth)));
    }

    @GetMapping("/itens-frete")
    public ResponseEntity<?> itensFrete(Authentication auth) {
        return ResponseEntity.ok(service.listarItensFrete(TenantUtils.getEmpresaId(auth)));
    }

    // --- WRITE: Rotas ---
    @PostMapping("/rotas")
    public ResponseEntity<?> criarRota(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarRota(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/rotas/{id}")
    public ResponseEntity<?> atualizarRota(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarRota(TenantUtils.getEmpresaId(auth), id, dados));
    }

    // --- WRITE: Embarcacoes ---
    @PostMapping("/embarcacoes")
    public ResponseEntity<?> criarEmbarcacao(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarEmbarcacao(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/embarcacoes/{id}")
    public ResponseEntity<?> atualizarEmbarcacao(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarEmbarcacao(TenantUtils.getEmpresaId(auth), id, dados));
    }

    // --- WRITE: Conferentes ---
    @PostMapping("/conferentes")
    public ResponseEntity<?> criarConferente(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarConferente(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/conferentes/{id}")
    public ResponseEntity<?> atualizarConferente(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarConferente(TenantUtils.getEmpresaId(auth), id, dados));
    }

    // --- WRITE: Caixas ---
    @PostMapping("/caixas")
    public ResponseEntity<?> criarCaixa(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarCaixa(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/caixas/{id}")
    public ResponseEntity<?> atualizarCaixa(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarCaixa(TenantUtils.getEmpresaId(auth), id, dados));
    }

    // --- WRITE: Clientes Encomenda ---
    @PostMapping("/clientes-encomenda")
    public ResponseEntity<?> criarClienteEncomenda(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarClienteEncomenda(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/clientes-encomenda/{id}")
    public ResponseEntity<?> atualizarClienteEncomenda(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarClienteEncomenda(TenantUtils.getEmpresaId(auth), id, dados));
    }

    // --- WRITE: Usuarios ---
    @PostMapping("/usuarios")
    public ResponseEntity<?> criarUsuario(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarUsuario(
            TenantUtils.getEmpresaId(auth), TenantUtils.isAdmin(auth), dados));
    }
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> atualizarUsuario(@PathVariable Integer id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarUsuario(
            TenantUtils.getEmpresaId(auth), TenantUtils.getOperadorId(auth),
            TenantUtils.isAdmin(auth), id, dados));
    }

    // --- WRITE: Tarifas ---
    @PostMapping("/tarifas")
    public ResponseEntity<?> criarTarifa(@RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.criarTarifa(TenantUtils.getEmpresaId(auth), dados));
    }
    @PutMapping("/tarifas/{id}")
    public ResponseEntity<?> atualizarTarifa(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        return ResponseEntity.ok(writeService.atualizarTarifa(TenantUtils.getEmpresaId(auth), id, dados));
    }
}
