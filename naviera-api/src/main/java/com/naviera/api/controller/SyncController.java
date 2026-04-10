package com.naviera.api.controller;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.SyncRequest;
import com.naviera.api.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "timestamp", Instant.now().toString()));
    }

    @PostMapping
    public ResponseEntity<?> sync(@RequestBody @Valid SyncRequest request, Authentication auth) {
        Integer empresaId = extractEmpresaId(auth);
        return ResponseEntity.ok(service.processar(request, empresaId));
    }

    @SuppressWarnings("unchecked")
    private Integer extractEmpresaId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof Map) {
            Object eid = ((Map<String, Object>) details).get("empresa_id");
            if (eid instanceof Integer) return (Integer) eid;
            if (eid != null) return Integer.valueOf(eid.toString());
        }
        throw ApiException.badRequest("empresa_id nao encontrado no token");
    }
}
