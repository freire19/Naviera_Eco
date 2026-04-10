package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/op/embarcacoes")
public class OpEmbarcacaoController {
    private final JdbcTemplate jdbc;

    public OpEmbarcacaoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        return ResponseEntity.ok(
            jdbc.queryForList("SELECT * FROM embarcacoes WHERE empresa_id = ? ORDER BY nome",
                TenantUtils.getEmpresaId(auth)));
    }
}
