package com.naviera.api.controller;

import com.naviera.api.dto.AmigoRequest;
import com.naviera.api.service.AmigoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/amigos")
public class AmigoController {
    private final AmigoService service;
    public AmigoController(AmigoService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.listar(id));
    }

    @GetMapping("/pendentes")
    public ResponseEntity<?> pendentes(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.pendentes(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(Authentication auth, @RequestParam String nome) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.buscarPorNome(id, nome));
    }

    @GetMapping("/sugestoes")
    public ResponseEntity<?> sugestoes(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.sugestoes(id));
    }

    @PostMapping("/{amigoId}")
    public ResponseEntity<?> enviarConvite(Authentication auth, @PathVariable Long amigoId) {
        Long id = (Long) auth.getPrincipal();
        service.enviarConvitePorId(id, amigoId);
        return ResponseEntity.ok(Map.of("mensagem", "Convite enviado"));
    }

    @PutMapping("/{amizadeId}/aceitar")
    public ResponseEntity<?> aceitar(Authentication auth, @PathVariable Long amizadeId) {
        Long id = (Long) auth.getPrincipal();
        service.aceitar(id, amizadeId);
        return ResponseEntity.ok(Map.of("mensagem", "Amizade aceita"));
    }

    @DeleteMapping("/{amizadeId}")
    public ResponseEntity<?> remover(Authentication auth, @PathVariable Long amizadeId) {
        Long id = (Long) auth.getPrincipal();
        service.remover(id, amizadeId);
        return ResponseEntity.ok(Map.of("mensagem", "Amizade removida"));
    }
}
