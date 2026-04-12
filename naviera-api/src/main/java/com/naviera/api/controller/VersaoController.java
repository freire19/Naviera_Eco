package com.naviera.api.controller;

import com.naviera.api.service.VersaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/versao")
public class VersaoController {
    private final VersaoService service;

    public VersaoController(VersaoService service) {
        this.service = service;
    }

    /**
     * Verifica se ha atualizacao disponivel para a plataforma informada.
     * Ex: GET /api/public/versao/check?plataforma=desktop&versaoAtual=1.0.0
     * Compatibilidade: param "v" continua funcionando se "versaoAtual" nao for enviado.
     */
    @GetMapping("/check")
    public ResponseEntity<?> check(
            @RequestParam(name = "plataforma", defaultValue = "desktop") String plataforma,
            @RequestParam(name = "versaoAtual", required = false) String versaoAtual,
            @RequestParam(name = "v", required = false) String v) {
        String versao = versaoAtual != null ? versaoAtual : (v != null ? v : "0.0.0");
        return ResponseEntity.ok(service.check(plataforma, versao));
    }

    /**
     * Retorna informacoes da versao atual do servidor para cada plataforma.
     * Ex: GET /api/public/versao/atual
     */
    @GetMapping("/atual")
    public ResponseEntity<?> versaoAtual() {
        return ResponseEntity.ok(service.versaoAtual());
    }
}
