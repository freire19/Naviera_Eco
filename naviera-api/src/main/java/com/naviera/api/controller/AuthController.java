package com.naviera.api.controller;

import com.naviera.api.dto.*;
import com.naviera.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try { return ResponseEntity.ok(service.login(req)); }
        catch (RuntimeException e) { return ResponseEntity.status(401).body(java.util.Map.of("erro", e.getMessage())); }
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody RegisterRequest req) {
        try { return ResponseEntity.ok(service.registrar(req)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage())); }
    }
}
