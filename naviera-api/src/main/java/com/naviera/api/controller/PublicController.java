package com.naviera.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;

@RestController @RequestMapping("/public")
public class PublicController {

    @Value("${naviera.uploads.dir:uploads}")
    private String uploadsDir;

    @GetMapping("/fotos/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) {
        try {
            // Sanitize filename to prevent path traversal
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
