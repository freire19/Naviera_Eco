package com.naviera.api.controller;

import com.naviera.api.service.OnboardingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.util.Map;

@RestController @RequestMapping("/public")
public class PublicController {

    @Value("${naviera.uploads.dir:uploads}")
    private String uploadsDir;

    private final OnboardingService onboarding;

    public PublicController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    // ========================================================================
    // ONBOARDING SELF-SERVICE
    // ========================================================================

    /**
     * Registro de empresa pelo site (self-service).
     * Cria empresa + primeiro usuario + codigo de ativacao.
     */
    @PostMapping("/registrar-empresa")
    public ResponseEntity<?> registrarEmpresa(@RequestBody Map<String, Object> dados) {
        return ResponseEntity.status(201).body(onboarding.registrarEmpresa(dados));
    }

    /**
     * Ativacao pelo Desktop: valida codigo e retorna dados da empresa.
     * Usado pelo SetupWizard simplificado (unico campo: codigo).
     *
     * SECURITY NOTE (#DB148): RateLimitFilter aplica limite geral de 200 req/min por IP,
     * mas este endpoint deveria ter um limite mais restrito (ex: 5 req/min por IP) para
     * resistir a brute-force mesmo com codigos de 8 hex. TODO: adicionar bucket dedicado
     * "ativar:" no RateLimitFilter (similar ao "login:" existente, max=5).
     */
    @GetMapping("/ativar/{codigo}")
    public ResponseEntity<?> ativarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(onboarding.ativarPorCodigo(codigo));
    }

    // ========================================================================
    // FOTOS
    // ========================================================================

    // #DS5-004: whitelist estrita de nome — bloqueia schemes file://, http://, path traversal
    // Aceita: perfil_<id>.(jpg|png|webp) (legado) e perfil_<id>_<uuid32>.(jpg|png|webp) (novo)
    private static final java.util.regex.Pattern FOTO_NAME =
        java.util.regex.Pattern.compile("^perfil_\\d+(?:_[a-f0-9]{32})?\\.(jpg|jpeg|png|webp)$");

    @GetMapping("/fotos/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) {
        try {
            String safe = Paths.get(filename).getFileName().toString();
            if (!FOTO_NAME.matcher(safe).matches()) return ResponseEntity.notFound().build();
            Path file = Paths.get(uploadsDir, "fotos", safe);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            String ct = safe.endsWith(".png") ? "image/png" : safe.endsWith(".webp") ? "image/webp" : "image/jpeg";
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
