package com.naviera.api.controller;

import com.naviera.api.service.PushService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/push")
public class PushController {
    private final PushService service;

    public PushController(PushService service) {
        this.service = service;
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> dados, Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        String tokenFcm = (String) dados.get("tokenFcm");
        String plataforma = (String) dados.getOrDefault("plataforma", "web");
        return ResponseEntity.ok(service.registrarDispositivo(clienteId, tokenFcm, plataforma));
    }

    @DeleteMapping("/desregistrar")
    public ResponseEntity<?> desregistrar(@RequestBody Map<String, Object> dados, Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        String tokenFcm = (String) dados.get("tokenFcm");
        return ResponseEntity.ok(service.desregistrar(clienteId, tokenFcm));
    }
}
