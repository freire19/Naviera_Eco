package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/op/rotas")
public class OpRotaController {
    private final JdbcTemplate jdbc;

    public OpRotaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        return ResponseEntity.ok(
            jdbc.queryForList("SELECT id_rota, origem, destino FROM rotas WHERE empresa_id = ? ORDER BY origem, destino",
                TenantUtils.getEmpresaId(auth)));
    }
}
