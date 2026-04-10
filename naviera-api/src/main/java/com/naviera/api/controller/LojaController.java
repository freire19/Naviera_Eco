package com.naviera.api.controller;

import com.naviera.api.service.LojaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/lojas")
public class LojaController {
    private final LojaService service;
    public LojaController(LojaService service) {
        this.service = service;
    }

    /** Lista todas as lojas parceiras ativas (CNPJ vê parceiros, CPF vê vitrine) */
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String cidade) {
        return ResponseEntity.ok(cidade != null ? service.listarPorCidade(cidade) : service.listarAtivas());
    }

    /** Minha loja (CNPJ logado) */
    @GetMapping("/minha")
    public ResponseEntity<?> minhaLoja(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        try { return ResponseEntity.ok(service.buscarMinhaLoja(id)); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    /** Pedidos recebidos pela minha loja (CNPJ) */
    @GetMapping("/pedidos")
    public ResponseEntity<?> pedidosDaLoja(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.pedidosDaLoja(id));
    }

    /** Minhas compras em lojas parceiras (CPF) */
    @GetMapping("/minhas-compras")
    public ResponseEntity<?> minhasCompras(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.minhasCompras(id));
    }

    /** Vincular pedido a um frete existente (CNPJ) */
    @PutMapping("/pedidos/{pedidoId}/vincular-frete")
    public ResponseEntity<?> vincularFrete(@PathVariable Long pedidoId, @RequestBody Map<String, Object> body) {
        Long idFrete = ((Number) body.get("idFrete")).longValue();
        String codigo = (String) body.getOrDefault("codigoRastreio", "");
        service.vincularFrete(pedidoId, idFrete, codigo);
        return ResponseEntity.ok(Map.of("mensagem", "Frete vinculado com sucesso"));
    }

    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<?> avaliacoes(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarAvaliacoes(id));
    }

    @PostMapping("/{id}/avaliacoes")
    public ResponseEntity<?> avaliar(@PathVariable Long id, @RequestBody Map<String, Object> dados, Authentication auth) {
        Long clienteId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.criarAvaliacao(id, clienteId, dados.get("nota"), dados.get("comentario")));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> stats(@PathVariable Long id) {
        return ResponseEntity.ok(service.stats(id));
    }
}
