package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.dto.CompraPassagemRequest;
import com.naviera.api.service.PassagemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/passagens")
public class PassagemController {
    private final PassagemService service;
    public PassagemController(PassagemService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> minhas(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.minhasPassagens(id));
    }

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(Authentication auth, @RequestBody @Valid CompraPassagemRequest req) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.comprar(id, req));
    }

    /** Operador escaneia QR — retorna dados do passageiro para conferencia visual */
    @GetMapping("/embarque/{numeroBilhete}")
    public ResponseEntity<?> consultarEmbarque(@PathVariable String numeroBilhete, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.consultarParaEmbarque(empresaId, numeroBilhete));
    }

    /** Operador confirma embarque apos conferir doc com foto */
    @PostMapping("/embarque/{numeroBilhete}/confirmar")
    public ResponseEntity<?> confirmarEmbarque(@PathVariable String numeroBilhete, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        String operador = auth.getCredentials() != null ? auth.getCredentials().toString() : "operador";
        return ResponseEntity.ok(service.confirmarEmbarque(empresaId, numeroBilhete, operador));
    }

    /** Operador confirma pagamento de passagem do app */
    @PostMapping("/{numeroBilhete}/confirmar-pagamento")
    public ResponseEntity<?> confirmarPagamento(@PathVariable String numeroBilhete, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.confirmarPagamento(empresaId, numeroBilhete));
    }
}
