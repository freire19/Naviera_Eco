package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.config.TenantUtils;
import com.naviera.api.dto.SyncRequest;
import com.naviera.api.service.SyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private static final Logger log = LoggerFactory.getLogger(SyncController.class);
    private final SyncService service;

    // #DS5-008: tabelas que mudam dinheiro/configuracao e exigem ROLE_ADMIN.
    //   Demais tabelas seguem liberadas pra ROLE_OPERADOR — operacao core do Desktop em campo.
    private static final Set<String> TABELAS_SO_ADMIN = Set.of(
        "financeiro_saidas",
        "tarifas"
    );

    public SyncController(SyncService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "timestamp", Instant.now().toString()));
    }

    @PostMapping
    public ResponseEntity<?> sync(@RequestBody @Valid SyncRequest request, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        String tabela = request.tabela();
        boolean admin = TenantUtils.isAdmin(auth) || TenantUtils.isSuperAdmin(auth);
        if (tabela != null && TABELAS_SO_ADMIN.contains(tabela.toLowerCase()) && !admin) {
            log.warn("Sync recusada [empresa_id={}, tabela={}, principal={}] — exige ROLE_ADMIN", empresaId, tabela, auth.getName());
            throw ApiException.forbidden("Tabela " + tabela + " requer permissao de admin");
        }
        log.info("Sync aceita [empresa_id={}, tabela={}, registros={}, principal={}]",
            empresaId, tabela, request.registros() == null ? 0 : request.registros().size(), auth.getName());
        return ResponseEntity.ok(service.processar(request, empresaId));
    }
}
