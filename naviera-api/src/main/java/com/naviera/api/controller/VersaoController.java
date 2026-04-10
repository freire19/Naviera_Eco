package com.naviera.api.controller;

import com.naviera.api.service.VersaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/versao")
public class VersaoController {
    private final VersaoService service;

    public VersaoController(VersaoService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam(name = "v", defaultValue = "0.0.0") String versao) {
        return ResponseEntity.ok(service.check(versao));
    }
}
