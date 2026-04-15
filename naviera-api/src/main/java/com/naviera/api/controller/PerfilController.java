package com.naviera.api.controller;

import com.naviera.api.dto.PerfilUpdateRequest;
import com.naviera.api.repository.ClienteAppRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController @RequestMapping("/perfil")
public class PerfilController {
    private final ClienteAppRepository repo;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB

    @Value("${naviera.uploads.dir:uploads}")
    private String uploadsDir;

    public PerfilController(ClienteAppRepository repo) { this.repo = repo; }

    @GetMapping
    public ResponseEntity<?> meuPerfil(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return repo.findById(id)
            .map(c -> {
                var m = new HashMap<String, Object>();
                m.put("id", c.getId()); m.put("nome", c.getNome()); m.put("documento", c.getDocumento());
                m.put("tipo", c.getTipoDocumento()); m.put("email", c.getEmail() != null ? c.getEmail() : "");
                m.put("telefone", c.getTelefone() != null ? c.getTelefone() : "");
                m.put("cidade", c.getCidade() != null ? c.getCidade() : "");
                m.put("fotoUrl", c.getFotoUrl() != null ? c.getFotoUrl() : "");
                return ResponseEntity.ok((Map<String, Object>) m);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping @Transactional
    public ResponseEntity<?> atualizar(Authentication auth, @RequestBody @Valid PerfilUpdateRequest dados) {
        Long id = (Long) auth.getPrincipal();
        return repo.findById(id).map(c -> {
            if (dados.nome() != null) c.setNome(dados.nome());
            if (dados.email() != null) c.setEmail(dados.email());
            if (dados.telefone() != null) c.setTelefone(dados.telefone());
            if (dados.cidade() != null) c.setCidade(dados.cidade());
            repo.save(c);
            return ResponseEntity.ok(Map.of("mensagem", "Perfil atualizado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/foto") @Transactional
    public ResponseEntity<?> uploadFoto(Authentication auth, @RequestParam("foto") MultipartFile file) throws IOException {
        Long id = (Long) auth.getPrincipal();
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo vazio"));
        if (!TIPOS_PERMITIDOS.contains(file.getContentType())) return ResponseEntity.badRequest().body(Map.of("erro", "Formato nao suportado. Use JPG, PNG ou WebP."));
        if (file.getSize() > MAX_SIZE) return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo muito grande. Maximo 2MB."));

        return repo.findById(id).map(c -> {
            try {
                Path dir = Paths.get(uploadsDir, "fotos"); Files.createDirectories(dir);
                String rawExt = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")) : ".jpg";
                // DB150: whitelist de extensoes — rejeita extensoes nao permitidas
                Set<String> allowedExts = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
                String ext = allowedExts.contains(rawExt.toLowerCase()) ? rawExt.toLowerCase() : ".jpg";
                String filename = "perfil_" + id + ext;
                Path dest = dir.resolve(filename);
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                String url = "/public/fotos/" + filename;
                c.setFotoUrl(url);
                repo.save(c);
                return ResponseEntity.ok(Map.of("fotoUrl", url));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao salvar foto"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
