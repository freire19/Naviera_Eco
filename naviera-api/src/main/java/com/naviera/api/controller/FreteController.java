package com.naviera.api.controller;

import com.naviera.api.dto.PagarFreteRequest;
import com.naviera.api.service.FreteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/fretes")
public class FreteController {
    private final FreteService service;
    public FreteController(FreteService service) { this.service = service; }

    // DS4-002 fix: cross-tenant — busca por nome do cliente em todas as empresas
    @GetMapping
    public ResponseEntity<?> meusFretes(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.buscarPorRemetenteCrossTenant(id));
    }

    /** Cliente CNPJ paga frete — PIX (10% off), CARTAO, BOLETO ou BARCO. */
    @PostMapping("/{idFrete}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Long idFrete,
                                   @RequestBody @Valid PagarFreteRequest req,
                                   Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.pagar(clienteId, idFrete, req.formaPagamento()));
    }
}
