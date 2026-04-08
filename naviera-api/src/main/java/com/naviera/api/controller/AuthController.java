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
        return ResponseEntity.ok(service.login(req));
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(service.registrar(req));
    }
}
