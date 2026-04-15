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
     * POST /api/bilhetes/comprar
     * Body: { "idViagem": 42, "idRota": 1, "idTipoPassagem": 1 }
     * Cria passagem + bilhete digital com TOTP
     */
    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(Authentication auth, @RequestBody Map<String, Object> body) {
        Long clienteId = (Long) auth.getPrincipal();
        Long idViagem = toLong(body.get("idViagem"));
        Long idRota = body.containsKey("idRota") ? toLong(body.get("idRota")) : null;
        Long idTipoPassagem = toLong(body.getOrDefault("idTipoPassagem", 1));
        // empresaId deve ser informado pelo app (ex: ao listar viagens públicas por empresa)
        Integer empresaId = body.get("empresaId") != null ? ((Number) body.get("empresaId")).intValue() : null;

        if (idViagem == null)
            return ResponseEntity.badRequest().body(Map.of("erro", "idViagem é obrigatório."));
        if (empresaId == null)
            return ResponseEntity.badRequest().body(Map.of("erro", "empresaId é obrigatório."));

        var bilhete = service.comprar(empresaId, clienteId, idViagem, idRota, idTipoPassagem);
        return ResponseEntity.ok(bilhete);
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
     * GET /api/bilhetes/totp?secret=xxx
     * Gera TOTP atual para um secret (usado pelo app para mostrar o código rotativo)
     */
    @GetMapping("/totp")
    public ResponseEntity<?> gerarTOTP(@RequestParam String secret) {
        long now = java.time.Instant.now().getEpochSecond();
        String code = BilheteService.generateTOTP(secret, now);
        int timeLeft = 30 - (int) (now % 30);
        return ResponseEntity.ok(Map.of("code", code, "timeLeft", timeLeft));
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
