package com.naviera.api.psp;

import com.naviera.api.config.TenantUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Gerencia subconta PSP da empresa logada.
 * Acessivel por operador admin (filtra pelo empresa_id do JWT).
 */
@RestController
@RequestMapping("/psp")
public class PspController {

    private final EmpresaPspService service;

    public PspController(EmpresaPspService service) {
        this.service = service;
    }

    /** GET /psp/status — retorna provider, subcontaId e status (SEM_SUBCONTA | ATIVA). */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.status(empresaId));
    }

    /** POST /psp/onboarding — cria subconta no PSP com dados da empresa + responsavel. */
    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(@RequestBody @Valid OnboardingRequest req, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        return ResponseEntity.ok(service.onboarding(empresaId, req));
    }
}
