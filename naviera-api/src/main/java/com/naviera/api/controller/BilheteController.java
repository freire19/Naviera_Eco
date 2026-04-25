package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.BilheteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bilhetes")
public class BilheteController {

    private final BilheteService service;

    public BilheteController(BilheteService service) {
        this.service = service;
    }

    /**
     * POST /api/bilhetes/comprar — DEPRECATED em 2026-04-24 (#716).
     *
     * Era uma implementacao paralela de compra de passagem que gravava valor_pago = valor_total
     * sem passar pelo PSP (passagem sai "paga" sem dinheiro efetivo). Divergente de
     * PassagemService.comprar (que tem desconto PIX, integracao Asaas, PENDENTE_CONFIRMACAO).
     *
     * App mobile ja usa POST /passagens/comprar. Retorna 410 Gone para desencorajar clientes
     * legados; mantido o metodo service.comprar por enquanto para nao quebrar testes.
     */
    @PostMapping("/comprar")
    public ResponseEntity<?> comprar() {
        return ResponseEntity.status(410).body(Map.of(
            "erro", "Endpoint removido. Use POST /api/passagens/comprar (com formaPagamento)."
        ));
    }

    /**
     * GET /api/bilhetes
     * Lista bilhetes do cliente logado
     */
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    /**
     * POST /api/bilhetes/validar
     * Body: { "qrHash": "abc123...", "totpCode": "482917" }
     * Valida bilhete escaneado pelo operador — requer ROLE_OPERADOR
     */
    @PostMapping("/validar")
    public ResponseEntity<?> validar(Authentication auth, @RequestBody Map<String, String> body) {
        // #DB146: only operators may validate bilhetes (scanner at boarding)
        boolean isOperador = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_OPERADOR".equals(a.getAuthority()));
        if (!isOperador) throw ApiException.forbidden("Somente operadores podem validar bilhetes");
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        String qrHash = body.get("qrHash");
        String totpCode = body.get("totpCode");
        String operador = body.getOrDefault("operador", "app");

        if (qrHash == null || totpCode == null)
            return ResponseEntity.badRequest().body(Map.of("erro", "qrHash e totpCode são obrigatórios."));

        var resultado = service.validar(empresaId, qrHash, totpCode, operador);
        return ResponseEntity.ok(Map.of("valido", true, "passageiro", resultado));
    }

    /**
     * GET /api/bilhetes/{id}/totp
     * DS4-007/DS4-018 fix: Gera TOTP server-side para bilhete do cliente autenticado.
     * Secret nunca sai do servidor — codigo gerado e retornado pronto.
     */
    @GetMapping("/{id}/totp")
    public ResponseEntity<?> gerarTOTP(@PathVariable Long id, Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.gerarTOTPPorBilhete(clienteId, id));
    }
}
