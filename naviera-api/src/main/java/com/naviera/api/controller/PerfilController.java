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
import java.util.UUID;

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
                byte[] data = file.getBytes();
                // #DS5-004: valida magic bytes — content-type header e falsificavel
                String realMime = detectMime(data);
                if (realMime == null || !TIPOS_PERMITIDOS.contains(realMime))
                    return ResponseEntity.badRequest().body(Map.of("erro", "Conteudo do arquivo nao bate com imagem jpg/png/webp"));
                String ext = ".jpg";
                if ("image/png".equals(realMime)) ext = ".png";
                else if ("image/webp".equals(realMime)) ext = ".webp";

                Path dir = Paths.get(uploadsDir, "fotos"); Files.createDirectories(dir);
                // #DS5-004: UUID no nome impede enumeracao /public/fotos/perfil_<N>.jpg
                String filename = "perfil_" + id + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
                Path dest = dir.resolve(filename);
                Files.write(dest, data, StandardOpenOption.CREATE_NEW);

                // limpa foto anterior (se existir e apontar pra uploads/fotos/)
                String oldUrl = c.getFotoUrl();
                if (oldUrl != null && oldUrl.startsWith("/public/fotos/")) {
                    String oldName = Paths.get(oldUrl.substring("/public/fotos/".length())).getFileName().toString();
                    try { Files.deleteIfExists(dir.resolve(oldName)); } catch (IOException ignored) {}
                }

                String url = "/public/fotos/" + filename;
                c.setFotoUrl(url);
                repo.save(c);
                return ResponseEntity.ok(Map.of("fotoUrl", url));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao salvar foto"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String detectMime(byte[] data) {
        if (data == null || data.length < 12) return null;
        // JPEG: FF D8 FF
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) return "image/jpeg";
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') return "image/png";
        // WebP: "RIFF" ???? "WEBP"
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
         && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') return "image/webp";
        return null;
    }
}
