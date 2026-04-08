package com.naviera.api.controller;

import com.naviera.api.repository.RotaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/rotas")
public class RotaController {
    private final RotaRepository repo;
    public RotaController(RotaRepository repo) { this.repo = repo; }

    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(repo.findAll()); }
}
