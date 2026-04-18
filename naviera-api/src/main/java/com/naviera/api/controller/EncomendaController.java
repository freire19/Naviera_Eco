package com.naviera.api.controller;

import com.naviera.api.dto.PagarEncomendaRequest;
import com.naviera.api.service.EncomendaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/encomendas")
public class EncomendaController {
    private final EncomendaService service;
    public EncomendaController(EncomendaService service) { this.service = service; }

    // DS4-002 fix: cross-tenant — busca por nome do cliente em todas as empresas
    // empresaId derivado server-side, nunca do request
    @GetMapping
    public ResponseEntity<?> minhasEncomendas(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.rastreioCrossTenant(id));
    }

    /** Rastreio cross-tenant — encomendas do cliente logado em todas as empresas */
    @GetMapping("/rastreio")
    public ResponseEntity<?> rastreio(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.rastreioCrossTenant(id));
    }

    /** Cliente CPF paga uma encomenda destinada a ele — PIX (10% off), CARTAO ou BARCO. */
    @PostMapping("/{idEncomenda}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Long idEncomenda,
                                   @RequestBody @Valid PagarEncomendaRequest req,
                                   Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.pagar(clienteId, idEncomenda, req.formaPagamento()));
    }
}
