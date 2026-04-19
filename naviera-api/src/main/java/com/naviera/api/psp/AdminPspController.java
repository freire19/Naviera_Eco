package com.naviera.api.psp;

import com.naviera.api.config.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints admin para super-admins operarem onboarding PSP em nome de
 * qualquer empresa (cadastram subconta Asaas de uma empresa cliente
 * sem precisar logar como admin daquela empresa).
 *
 * Protegido: exige ROLE_ADMIN no JWT (claim funcao = Administrador/ADMIN).
 * A rota vive em /api/admin/empresas/{empresaId}/psp/... pra deixar claro
 * que trabalha sobre qualquer empresa, nao sobre a do usuario logado.
 */
@RestController
@RequestMapping("/admin/empresas/{empresaId}/psp")
public class AdminPspController {

    private final EmpresaPspService service;

    public AdminPspController(EmpresaPspService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@PathVariable Integer empresaId, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(service.status(empresaId));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(@PathVariable Integer empresaId,
                                        @RequestBody @Valid OnboardingRequest req,
                                        Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(service.onboarding(empresaId, req));
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null) throw ApiException.forbidden("Autenticacao obrigatoria");
        boolean isAdmin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(r -> "ROLE_ADMIN".equals(r));
        if (!isAdmin) throw ApiException.forbidden("Apenas administradores podem operar onboarding de outras empresas");
    }
}
