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
     */
    @GetMapping("/ativar/{codigo}")
    public ResponseEntity<?> ativarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(onboarding.ativarPorCodigo(codigo));
    }

    // ========================================================================
    // FOTOS
    // ========================================================================

    @GetMapping("/fotos/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) {
        try {
            String safe = Paths.get(filename).getFileName().toString();
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
