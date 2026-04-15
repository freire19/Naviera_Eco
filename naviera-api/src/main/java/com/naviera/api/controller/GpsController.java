package com.naviera.api.controller;

import com.naviera.api.config.TenantUtils;
import com.naviera.api.service.GpsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class GpsController {
    private final GpsService service;

    public GpsController(GpsService service) {
        this.service = service;
    }

    /** Tripulação envia posição GPS (requer ROLE_OPERADOR) */
    @PostMapping("/gps/posicao")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> dados, Authentication auth) {
        Integer empresaId = TenantUtils.getEmpresaId(auth);
        Long idEmbarcacao = ((Number) dados.get("id_embarcacao")).longValue();
        Long idViagem = dados.get("id_viagem") != null ? ((Number) dados.get("id_viagem")).longValue() : null;
        double lat = ((Number) dados.get("latitude")).doubleValue();
        double lon = ((Number) dados.get("longitude")).doubleValue();
        Double velocidade = dados.get("velocidade") != null ? ((Number) dados.get("velocidade")).doubleValue() : null;
        Double curso = dados.get("curso") != null ? ((Number) dados.get("curso")).doubleValue() : null;

        // DS4-033 fix: validar ranges de coordenadas
        if (lat < -90 || lat > 90) return ResponseEntity.badRequest().body(Map.of("erro", "Latitude deve estar entre -90 e 90"));
        if (lon < -180 || lon > 180) return ResponseEntity.badRequest().body(Map.of("erro", "Longitude deve estar entre -180 e 180"));

        return ResponseEntity.ok(service.registrarPosicao(empresaId, idEmbarcacao, idViagem, lat, lon, velocidade, curso));
    }

    /** Última posição de uma embarcação (público) */
    @GetMapping("/embarcacoes/{id}/gps")
    public ResponseEntity<?> ultimaPosicao(@PathVariable Long id) {
        return ResponseEntity.ok(service.ultimaPosicao(id));
    }

    /** Histórico de posições de uma viagem (público) */
    @GetMapping("/viagens/{id}/rastreio")
    public ResponseEntity<?> rastreio(@PathVariable Long id) {
        return ResponseEntity.ok(service.historicoViagem(id));
    }

    /** Última posição de todas as embarcações (público, para mapa de tracking) */
    @GetMapping("/gps/embarcacoes")
    public ResponseEntity<?> todasPosicoes() {
        return ResponseEntity.ok(service.todasUltimasPosicoes());
    }
}
