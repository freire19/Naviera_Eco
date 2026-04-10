package com.naviera.api.controller;

import com.naviera.api.dto.LoginOperadorRequest;
import com.naviera.api.service.AuthOperadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/operador")
public class AuthOperadorController {
    private final AuthOperadorService service;

    public AuthOperadorController(AuthOperadorService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginOperadorRequest req) {
        return ResponseEntity.ok(service.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.me(id));
    }
}
